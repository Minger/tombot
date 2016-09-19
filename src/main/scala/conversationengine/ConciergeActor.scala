package conversationengine

import java.util.concurrent.TimeoutException

import akka.actor._
import akka.pattern.after
import akka.stream.Materializer
import apis.ciscospark.SparkTempMembership
import com.google.inject.Inject
import com.google.inject.name.Named
import com.typesafe.config.Config
import conversationengine.AgentConversationActor.{SparkMessageEvent, SparkRoomLeftEvent, SparkWrappedEvent}
import conversationengine.ConciergeActor.{Data, State}
import conversationengine.ConversationActor.{Exchange, Respond, TextLike}
import conversationengine.FormActor.NextQuestion
import modules.akkaguice.GuiceAkkaExtension
import services._

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._
import scala.concurrent.duration._

/**
  * Created by markmo on 9/09/2016.
  */
class ConciergeActor @Inject()(config: Config,
                               @Named(FacebookService.name) provider: MessagingProvider,
                               sparkService: SparkService,
                               alchemyService: AlchemyService,
                               rulesService: RulesService,
                               implicit val system: ActorSystem,
                               implicit val fm: Materializer)
  extends Actor with ActorLogging with FSM[State, Data] {

  import ConciergeActor._
  import rulesService._
  import controllers.Platforms._

  implicit val timeout = 20 second

  implicit class FutureExtensions[T](f: Future[T]) {
    def withTimeout(timeout: => Throwable)(implicit duration: FiniteDuration, system: ActorSystem): Future[T] = {
      Future firstCompletedOf Seq(f, after(duration, system.scheduler)(Future.failed(timeout)))
    }
  }

  val conversationEngine = config.getString("conversation.engine")

  // bot conversation actor
  val bot = if (conversationEngine == "watson") {
    context.actorOf(GuiceAkkaExtension(context.system).props(WatsonConversationActor.name))
  } else {
    context.actorOf(GuiceAkkaExtension(context.system).props(IntentActor.name))
  }

  // form conversation actor
  val form = context.actorOf(GuiceAkkaExtension(context.system).props(FormActor.name))

  // agent conversation actor
  val agent = context.actorOf(GuiceAkkaExtension(context.system).props(AgentConversationActor.name))

  val agentName = "Mark"

  val tempMemberships = mutable.Map[String, SparkTempMembership]()

  startWith(UsingBot, Uninitialized)

  when(UsingBot) {

    case Event(ev: TextLike, _) =>
      log.debug("received TextLike event")
      val sender = ev.sender
      val text = ev.text

      if (text startsWith "/alchemy") {
        // alchemy command - show keywords
        val keywords = alchemyService.getKeywords(text.substring(8).trim)
        provider.sendTextMessage(sender, "Keywords:\n" + formatKeywords(keywords))

      } else if (isQuestion(text)) {
        // hears question
        getContent(text) match {

          case Some(content) =>
            log.debug("found content")
            provider.sendTextMessage(sender, content)

          case None =>
            // no content found
            bot ! Respond(Facebook, sender, text)

        }
      } else {
        bot ! Respond(Facebook, sender, text)
      }
      stay

    case Event(Fallback(sender, history), _) =>
      log.debug("falling back")
      provider.sendTextMessage(sender, s"$agentName (Human) is joining the conversation")
      lazy val fut = sparkService.setupTempRoom(sender) withTimeout new TimeoutException("future timed out")
      val tempMembership = Await.result(fut, timeout)
      tempMemberships(sender) = tempMembership

      // print transcript history
      history map {
        case Exchange(Some(userSaid), botSaid) => s"user: $userSaid\ntombot: $botSaid"
        case Exchange(None, botSaid) => s"tombot: $botSaid"
      } foreach { text =>
        sparkService.postMessage(Some(tempMembership.roomId), None, None, text, None)

        // TODO
        // implement message send queue - send next after acknowledgement
        // hack due to messages posting out of sequence
        Thread.sleep(2000)
      }
      goto(UsingHuman)

    case Event(FillForm(sender, goal), _) =>
      log.debug("received FillForm event")
      form ! NextQuestion(sender)
      stay

    case Event(EndFillForm(sender), _) =>
      log.debug("received EndFillForm event")
      // TODO
      stay

    case Event(ev, _) =>
      log.debug("received event")
      bot ! ev
      stay
  }

  when(UsingHuman) {

    case Event(ev: SparkMessageEvent, _) =>
      log.debug("received spark message event")
      agent ! ev
      stay

    case Event(SparkRoomLeftEvent(sender), _) =>
      log.debug("received room left event")
      provider.sendTextMessage(sender, s"$agentName (Human) is leaving the conversation")
      val tempMembership = tempMemberships(sender)
      sparkService.deleteWebhook(tempMembership.leaveRoomWebhookId)
      sparkService.deleteWebhook(tempMembership.webhookId)
      sparkService.deleteTeam(tempMembership.teamId)
      goto(UsingBot)

    case Event(ev: TextLike, _) =>
      log.debug("received facebook event")
      val tempMembership = tempMemberships(ev.sender)
      agent ! SparkWrappedEvent(tempMembership.roomId, tempMembership.personId, ev)
      stay
  }


  private def formatKeywords(keywords: Map[String, Double]) = {
    keywords map {
      case (keyword, relevance) => f"$keyword ($relevance%2.2f)"
    } mkString "\n"
  }

}

object ConciergeActor extends modules.akkaguice.NamedActor {

  override final val name = "ConciergeActor"

  case class Fallback(sender: String, history: List[Exchange])
  case class FillForm(sender: String, goal: String)
  case class EndFillForm(sender: String)

  sealed trait State
  case object UsingBot extends State
  case object UsingHuman extends State

  sealed trait Data
  case object Uninitialized extends Data

}
package memory

import javax.script.ScriptEngineManager

import com.typesafe.config.{ConfigFactory, ConfigObject, ConfigValue}
import jdk.nashorn.api.scripting.JSObject

import scala.collection.JavaConversions._

/**
  * Created by markmo on 30/07/2016.
  */
case class Slot(key: String,
                question: Option[String] = None,
                children: Option[List[Slot]] = None,
                value: Option[Any] = None,
                parseFn: Option[(String) => Map[String, Any]] = None) {

  def nextQuestion: Option[(String, String)] =
    if (value.isEmpty) {
      if (children.isDefined) {
        val qs = children.get.map(_.nextQuestion)

        if (qs.forall(_.isEmpty)) {
          // if all child slots have been filled then this parent slot is done
          None

        } else if (question.isDefined && qs.forall(_.isDefined)) {
          // if all child slots have questions (and therefore need to be filled),
          // and a question is defined at the parent slot (and therefore a
          // composite response can be given), then ask the next question from
          // this slot
          Some((key, question.get))

        } else {
          // if a composite response is not possible, or some, but not all,
          // child slots have already been answered, then ask the next question
          // from the first unanswered child slot
          qs.find(_.isDefined).get
        }
      } else {
        if (question.isDefined) {
          // if no child slots then ask this question if defined
          Some((key, question.get))
        } else {
          None
        }
      }
    } else {
      None
    }

  def fillSlotNext(key: String, value: Any): Slot = fillSlot(key, value).get

  def fillSlot(key: String, value: Any): Option[Slot] =
    if (this.key == key) {
      if (children.isDefined && parseFn.isDefined) {
//        val engine = new ScriptEngineManager().getEngineByMimeType("text/javascript")
//        val fn = engine.eval(parseFn).asInstanceOf[JSObject]
//        val result = fn.call(null, value)
        val result = parseFn.get(value.toString)
        val slots = children.get flatMap { child =>
          val k = child.key
          if (result.contains(k)) {
            child.fillSlot(k, result(k))
          } else {
            Some(child)
          }
        }
        Some(Slot(key, question, Some(slots.toList), Some(value)))
      } else {
        Some(Slot(key, question, children, Some(value)))
      }
    } else if (children.isDefined) {
      val slots = children.get flatMap {
        _.fillSlot(key, value)
      }
      Some(Slot(this.key, question, Some(slots), this.value))
    } else {
      Some(Slot(this.key, question, children, this.value))
    }

  def getValue(key: String): Option[Any] =
    if (this.key == key) {
      value
    } else if (children.isDefined) {
      children.get.map(_.getValue(key)).head
    } else {
      None
    }

  def getString(key: String): String =
    getValue(key).get.toString

  override def toString = toJson(1)

  def toJson(level: Int = 1): String = {
    val i = "  "
    val t = i * level
    val b = "\n"
    val s = "{" + b + t
    val n = "," + b + t

    def q(v: String) = "\"" + v + "\""

    def k(v: String) = q(v) + ": "

    var j = k(key) + s
    if (value.nonEmpty) {
      j += n + k("value") + q(value.get.toString)
    }
    if (question.nonEmpty) {
      j += n + k("question") + q(question.get)
    }
    if (children.nonEmpty) {
      j += n + children.get.map(_.toJson(level + 1)).mkString(n)
    }
    j + b + (i * (level - 1)) + "}"
  }

}

/*
object Slot {

  def create(key: String): Slot = {
    val config = ConfigFactory.load(s"forms/$key.conf")
    val root = config.getObject(key)

    import utils.ConfigOptional._

    def loop(key: String, value: ConfigValue): Slot = value match {
      case obj: ConfigObject =>
        val conf = obj.toConfig
        val dataType = conf.getString("type").toLowerCase
        val children = if (dataType == "object") {
          val slots = obj filterNot {
            case (k, _) => Set("type", "question") contains k
          } map {
            case (k, v) => loop(k, v)
          }
          Some(slots.toList)
        } else {
          None
        }

        Slot(
          dataType = dataType,
          key = key,
          question = conf.getOptionalString("question"),
          children = children,
          value = None
        )
    }

    loop(key, root)
  }

}*/

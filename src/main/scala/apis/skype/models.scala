package apis.skype

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import spray.json.lenses.JsonLenses._

/**
  * Created by markmo on 10/08/2016.
  */
case class SkypeSender(id: String, name: String)

case class SkypeRecipient(id: String, name: String)

case class SkypeConversation(id: String, name: Option[String], isGroup: Option[Boolean])

case class SkypeImage(url: String)

sealed trait SkypeButton {
  val buttonType: String
  val title: String
  val value: String
}

case class SkypeSigninButton(title: String, value: String) extends SkypeButton {
  override val buttonType = "signin"
}

case class SkypeLinkButton(title: String, value: String) extends SkypeButton {
  override val buttonType = "openUrl"
}

case class SkypeSigninAttachmentContent(text: String, buttons: List[SkypeSigninButton])

case class SkypeHeroAttachmentContent(title: Option[String],
                                      subtitle: Option[String],
                                      images: Option[List[SkypeImage]],
                                      buttons: List[SkypeButton])

sealed trait SkypeAttachment {
  val contentType: String
}

case class SkypeSigninAttachment(content: SkypeSigninAttachmentContent) extends SkypeAttachment {
  override val contentType = "application/vnd.microsoft.card.signin"
}

case class SkypeHeroAttachment(content: SkypeHeroAttachmentContent) extends SkypeAttachment {
  override val contentType = "application/vnd.microsoft.card.hero"
}

case class SkypeUserMessage(id: String,
                            messageType: String,
                            timestamp: String,
                            text: String,
                            channelId: String,
                            serviceUrl: String,
                            conversation: SkypeConversation,
                            from: SkypeSender,
                            recipient: SkypeRecipient,
                            attachments: Option[List[SkypeAttachment]],
                            entities: Option[List[JsObject]])

case class SkypeBotMessage(messageType: String, text: String, attachments: Option[List[SkypeAttachment]])

sealed trait SkypeAttachmentLayout {
  val attachmentLayout: String
  val attachments: List[SkypeAttachment]
}

case class SkypeCarousel(attachments: List[SkypeAttachment]) extends SkypeAttachmentLayout {
  override val attachmentLayout = "carousel"
}

case class SkypeList(attachments: List[SkypeAttachment]) extends SkypeAttachmentLayout {
  override val attachmentLayout = "list"
}

case class SkypeSigninCard(cardType: String, attachments: List[SkypeSigninAttachment])

case class MicrosoftToken(tokenType: String, expires: Int, extExpires: Int, accessToken: String)

trait SkypeJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val skypeSenderJsonFormat = jsonFormat2(SkypeSender)
  implicit val skypeRecipientJsonFormat = jsonFormat2(SkypeRecipient)
  implicit val skypeConversationJsonFormat = jsonFormat3(SkypeConversation)
  implicit val skypeImageJsonFormat = jsonFormat1(SkypeImage)
  implicit val skypeSigninButtonJsonFormat = jsonFormat2(SkypeSigninButton)
  implicit val skypeLinkButtonJsonFormat = jsonFormat2(SkypeLinkButton)

  implicit object skypeButtonJsonFormat extends RootJsonFormat[SkypeButton] {

    def write(b: SkypeButton) =
      JsObject(
        "type" -> JsString(b.buttonType),
        "title" -> JsString(b.title),
        "value" -> JsString(b.value)
      )

    def read(value: JsValue) =
      value.extract[String]('type) match {
        case "signin" =>
          SkypeSigninButton(value.extract[String]('title), value.extract[String]('value))
        case "openUrl" =>
          SkypeLinkButton(value.extract[String]('title), value.extract[String]('value))
        case _ => throw DeserializationException("SkypeButton expected")
      }

  }

  implicit val skypeSigninAttachmentContentJsonFormat = jsonFormat2(SkypeSigninAttachmentContent)
  implicit val skypeHeroAttachmentContentJsonFormat = jsonFormat4(SkypeHeroAttachmentContent)
  implicit val skypeSigninAttachmentJsonFormat = jsonFormat1(SkypeSigninAttachment)
  implicit val skypeHeroAttachmentJsonFormat = jsonFormat1(SkypeHeroAttachment)

  implicit object skypeAttachmentJsonFormat extends RootJsonFormat[SkypeAttachment] {

    def write(a: SkypeAttachment) = a match {
      case s: SkypeSigninAttachment => JsObject(
        "contentType" -> JsString(s.contentType),
        "content" -> s.content.toJson
      )
      case h: SkypeHeroAttachment => JsObject(
        "contentType" -> JsString(h.contentType),
        "content" -> h.content.toJson
      )
    }

    def read(value: JsValue) =
      value.extract[String]('contentType) match {
        case "application/vnd.microsoft.card.signin" =>
          SkypeSigninAttachment(value.extract[SkypeSigninAttachmentContent]('content))
        case "application/vnd.microsoft.card.hero" =>
          SkypeHeroAttachment(value.extract[SkypeHeroAttachmentContent]('content))
        case _ => throw DeserializationException("SkypeAttachment expected")
      }

  }

  implicit object skypeAttachmentLayoutJsonFormat extends RootJsonFormat[SkypeAttachmentLayout] {

    def write(l: SkypeAttachmentLayout) =
      JsObject(
        "attachmentLayout" -> JsString(l.attachmentLayout),
        "attachments" -> l.attachments.toJson
      )

    def read(value: JsValue) =
      value.extract[String]('attachmentLayout) match {
        case "carousel" =>
          SkypeCarousel(value.extract[List[SkypeAttachment]]('attachments))
        case "list" =>
          SkypeList(value.extract[List[SkypeAttachment]]('attachments))
        case _ => throw DeserializationException("SkypeAttachmentLayout expected")
      }

  }

  implicit val skypeUserMessageJsonFormat = jsonFormat(SkypeUserMessage, "id", "type", "timestamp", "text", "channelId", "serviceUrl", "conversation", "from", "recipient", "attachments", "entities")
  implicit val skypeBotMessageJsonFormat = jsonFormat(SkypeBotMessage, "type", "text", "attachments")
  implicit val skypeSigninCardJsonFormat = jsonFormat(SkypeSigninCard, "type", "attachments")
  implicit val microsoftTokenJsonFormat = jsonFormat(MicrosoftToken, "token_type", "expires_in", "ext_expires_in", "access_token")

}
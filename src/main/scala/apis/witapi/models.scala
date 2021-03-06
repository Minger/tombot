package apis.witapi

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

/**
  * Created by markmo on 26/07/2016.
  */
case class Entity(confidence: Double, value: String, entityType: Option[String])

case class Meaning(messageId: String, text: String, entities: Map[String, List[Entity]]) {

  def getIntent: Option[Entity] =
    if (entities.contains("intent")) {
      entities("intent").headOption
    } else {
      None
    }

  def getEntityValue(entity: String): Option[String] =
    entities
      .filter(x => x._1 == entity && x._2.nonEmpty)
      .map(_._2.head.value)
      .headOption

}

trait WitJsonSupport extends DefaultJsonProtocol with SprayJsonSupport {
  implicit val entityJsonFormat = jsonFormat(Entity, "confidence", "value", "type")
  implicit val meaningJsonFormat = jsonFormat(Meaning, "msg_id", "_text", "entities")
}
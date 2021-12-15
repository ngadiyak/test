import play.api.libs.json.Json

case class OrderBookUpdate (T: String, S: String, `_`: Long, U: Int, a: Option[List[List[String]]], b: Option[List[List[String]]])

object OrderBookUpdate {
  implicit val format = Json.format[OrderBookUpdate]
}


import play.api.libs.json._


case class SubscribeMessage(T: String = "obs", S: String = "WAVES-DG2xFkPdDwKUoBkzGAhQtLpSGzfXLiCYPEzeKH2Ad24p", d: Int = 1)

object SubscribeMessage {
  implicit val format = Json.format[SubscribeMessage]
}
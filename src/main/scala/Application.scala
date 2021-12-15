import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.{Done, NotUsed}
import akka.http.scaladsl.Http
import akka.stream.scaladsl._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.ws.{TextMessage, _}
import akka.stream.OverflowStrategy
import play.api.libs.functional.syntax.toFunctionalBuilderOps
import play.api.libs.json.{JsPath, Json, Reads}
import sttp.client3.{HttpURLConnectionBackend, UriContext, basicRequest}
import sttp.client3.playJson.asJson

import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.language.postfixOps

case class WsMessage(T: String, `_`: Long)

case class Pair(amountAsset: String, priceAsset: String)
case class Order(amount: Long, price: Long)

object Pair {
  implicit val format = Json.format[Pair]
}
object Order {
  implicit val format = Json.format[Order]
}

case class OrderBook(timestamp: Long, pair: Pair, asks: List[Order], bids: List[Order])

object OrderBook {
  implicit val format = Json.format[OrderBook]
}


object Application extends App {

  @volatile private var u = 0

  implicit val system = ActorSystem()

  import system.dispatcher

  implicit val format = Json.format[WsMessage]

  val backend = HttpURLConnectionBackend()

  def subscribe(ws: ActorRef): Unit = {
    ws ! TextMessage.Strict(Json.toJson(SubscribeMessage()).toString())
  }

  def handle(ws: ActorRef, m: Message): Future[Unit] = {
    m match {
      case TextMessage.Strict(msg) =>
        val wsm = Json.parse(msg).as[WsMessage]
        wsm.T match {
          case "ob" =>
            u = u + 1
            val uu = Json.parse(msg).as[OrderBookUpdate]
            println(s"ob -- ${u} : ${uu.U}")
            Future.successful(())
          case "pp" =>

            ws ! TextMessage.Strict(Json.toJson(wsm).toString())
            println(m)
            Future.successful(())

          case "i" => println("i"); Future.successful(())

          case _ => println(s"${msg} - !!!"); Future.successful(())
        }

      case TextMessage.Streamed(msg) =>
        println(s"Streamed: $msg"); Future.successful(())
      case _ =>
        throw new RuntimeException(s"Unexpected message $m")
    }
  }

  implicit val femaleNameReads: Reads[WsMessage] = (
    (JsPath \ "T").read[String] and
      (JsPath \ "_").read[Long]
    )(WsMessage.apply _)


  def runCon(flow: Flow[Message, Message, Future[WebSocketUpgradeResponse]]): Unit = {

    flow alsoTo Sink.onComplete(e => {
      println(e)
      runCon(flow)
    }) via flow


    var ws: ActorRef = Actor.noSender

    val ((ws1: ActorRef, upgradeResponse), _) =
      Source.actorRef[TextMessage.Strict](bufferSize = 10, OverflowStrategy.fail)
        .viaMat(webSocketFlow)(Keep.both)
        .toMat(
          Flow[Message]
            .mapAsync(1)(m => handle(ws, m))
            .to(Sink.ignore)
        )(Keep.both)
        .run()

    ws = ws1

    subscribe(ws)

    val connected = upgradeResponse.flatMap { upgrade =>
      if (upgrade.response.status == StatusCodes.SwitchingProtocols) {

        Future.successful(Done)
      } else {

        throw new RuntimeException(s"Connection failed: ${upgrade.response.status}")
      }
    }
  }



  def isComplete(): Future[Boolean] = {
    val r =
      basicRequest
        .get(uri"https://matcher.waves.exchange/matcher/orderbook/WAVES/DG2xFkPdDwKUoBkzGAhQtLpSGzfXLiCYPEzeKH2Ad24p?depth=10")
        .response(asJson[OrderBook])
        .send(backend)



    r.body match {
      case Left(ex) => println(ex); Future.successful(false)
      case Right(e) => println(e.bids.head);Future.successful(false)
    }

    Future.successful(false)
  }


  val stream: Future[Option[Boolean]] =
    Source(1 to Int.MaxValue)
      .throttle(1, 1 seconds)
      .mapAsync(parallelism = 1)(_ => isComplete())
      .takeWhile(_ == false, true)
      .runWith(Sink.lastOption)


  val webSocketFlow = Http().webSocketClientFlow(WebSocketRequest(uri = "wss://matcher.waves.exchange/ws/v0"))


  runCon(webSocketFlow)


  //  ws ! TextMessage.Strict("Hello World")
  //  ws ! TextMessage.Strict("Hi")
  //  ws ! TextMessage.Strict("Yay!")

}

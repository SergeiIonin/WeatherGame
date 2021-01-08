package weathergame

import scala.concurrent.{ExecutionContextExecutor, Future}
import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import com.typesafe.config.{Config, ConfigFactory}
import weathergame.restapi.RestApi

object Main extends App
    with RequestTimeout {

  val config = ConfigFactory.load() 
  val host = config.getString("http.host")
  val port = config.getInt("http.port")

  implicit val system = ActorSystem()  // ActorMaterializer requires an implicit ActorSystem
  implicit val ec: ExecutionContextExecutor = system.dispatcher  // bindingFuture.map requires an implicit ExecutionContext
  implicit val materializer = ActorMaterializer()  // bindAndHandle requires an implicit materializer

  val api: Route = new RestApi(system, requestTimeout(config)).routes // the RestApi provides a Route

  val bindingFuture: Future[ServerBinding] =
    Http().bindAndHandle(api, host, port) //Starts the HTTP server
 
  val log =  Logging(system.eventStream, "weather-game")
  bindingFuture.map { serverBinding =>
    log.info(s"RestApi bound to ${serverBinding.localAddress} ")
  }.recoverWith {
    case ex: Exception =>
      log.error(ex, s"Failed to bind to $host:$port!")
      system.terminate()
  }
}

trait RequestTimeout {
  import scala.concurrent.duration._
  def requestTimeout(config: Config): Timeout = {
    val t = config.getString("akka.http.server.request-timeout")
    val d = Duration(t)
    FiniteDuration(d.length, d.unit)
  }
}

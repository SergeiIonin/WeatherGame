package weathergame.openweather

import akka.actor.{Actor, Props}
import weathergame.openweather.WebWeatherCollectorActor.GetRealWeather
import weathergame.auth._
import java.nio.charset.Charset

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import spray.json._
import weathergame.weather.RealWeather

import scala.concurrent.{ExecutionContextExecutor, Future}

object WebWeatherCollectorClient {

  val appid = AuthProvider.appid
  val apiUrl = AuthProvider.apiUrl
  case class GetRealWeather(region: String, date: String)

}


class WebWeatherCollector()(
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer,
  implicit val executionContext: ExecutionContextExecutor
) extends ManifestJsonSupport {

  val nasaApiUrl = "https://api.nasa.gov/mars-photos/api/v1"
  val key = "c3ceLKYs5kNldaJDZ7eUJpi9UVvdfXZz1l0NpFQP"

  def getManifest(rover: String): Future[Info] = {
    val responseFuture: Future[HttpResponse] =
      Http().singleRequest(HttpRequest(uri = s"$nasaApiUrl/manifests/$rover?api_key=$key"))

    responseFuture.flatMap { res: HttpResponse =>
      res.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map { body =>
        val manifest = body.decodeString(Charset.defaultCharset()).parseJson.convertTo[RealWeather]
        Info(
          manifest.photo_manifest.max_sol,
          manifest.photo_manifest.name,
          manifest.photo_manifest.landing_date,
          manifest.photo_manifest.launch_date,
          manifest.photo_manifest.max_date,
          manifest.photo_manifest.status,
          manifest.photo_manifest.photos.maxBy(_.sol).cameras
        )
      }
    }
  }

class WebWeatherCollectorActor(name: String) extends Actor {
  override def receive: Receive = {
    case GetRealWeather(region, date) => {

    }
  }
}

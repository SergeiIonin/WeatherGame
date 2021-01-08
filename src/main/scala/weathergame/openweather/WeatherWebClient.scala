package weathergame.openweather

import akka.actor.{Actor, Props}
import weathergame.auth._
import java.nio.charset.Charset

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.{DateTime, HttpRequest, HttpResponse}
import akka.stream.ActorMaterializer
import akka.util.ByteString
import spray.json._
import weathergame.marshalling.WeatherServiceMarshaller
import weathergame.weather.Weather
import weathergame.weather.WeatherTypes.{Hail, NoPrecipitation, Precipitation, Rain, Snow}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, ExecutionContextExecutor, Future}

class WeatherWebClient()(
  implicit val system: ActorSystem,
  implicit val materializer: ActorMaterializer,
  implicit val executionContext: ExecutionContextExecutor
) extends OpenWeatherImpl with WeatherServiceMarshaller {

  case class Request(id: String = "", date: Option[String] = Some(DateTime.now.toString), location: Option[String] = Some("London,+uk"))

  def getWeather(req: Request): Future[Weather] = {
    val locationCode = Locations.locationIdToLocationCode(req.location.get)
    val responseFuture: Future[HttpResponse] =
      Http().singleRequest(HttpRequest(uri = s"$appUrl${locationCode}&APPID=$key"))

    responseFuture.flatMap { res: HttpResponse =>
      val jsValue: Future[JsValue] = res.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map { body =>
        body.decodeString(Charset.defaultCharset()).parseJson
      }

      val futMapping: Future[Map[String, JsValue]] = jsValue.map(_.asInstanceOf[JsObject].fields)

      futMapping.map {
        mapping =>
          val main = mapping("main").asJsObject.fields //"main" is the key in the json from the http-response
          val temperature = Some(main.getOrElse("temp", None).asInstanceOf[JsNumber].value.toInt - 273)  /*consider that
          the temperature should be centigrade*/


          val humidity = Some(main.getOrElse("humidity", None).asInstanceOf[JsNumber].value.toInt)

          val wind: Map[String, JsValue] = mapping("wind").asJsObject.fields
          val windSpeed = Some(wind.getOrElse("speed", None).asInstanceOf[JsNumber].value.toInt)

          val precipitationDescription =
            mapping("weather").asInstanceOf[JsArray]
              .elements.head.asInstanceOf[JsObject].fields
              .getOrElse("description", None).asInstanceOf[JsString].value
          val precipitation: Option[Precipitation] = precipitationDescription match {
            case s:String if s.toLowerCase.contains("rain") => Some(Rain())
            case s:String if s.toLowerCase.contains("snow") => Some(Snow())
            case s:String if s.toLowerCase.contains("hail") => Some(Hail())
            case _ => Some(NoPrecipitation())
          }

          val location = Some(mapping("name").asInstanceOf[JsString].value)
          Weather(id = req.id, temperature = temperature, precipitation = precipitation, humidity = humidity,
            wind = windSpeed, location = location)
      }
    }
  }
}

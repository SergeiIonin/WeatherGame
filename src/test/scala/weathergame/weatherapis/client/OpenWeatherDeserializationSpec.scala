package weathergame.weatherapis.client

import java.nio.charset.Charset

import akka.http.scaladsl.model.{ContentTypes, DateTime, HttpEntity, HttpResponse}
import akka.http.scaladsl.server.ContentNegotiator.Alternative.ContentType
import akka.http.scaladsl.testkit.ScalatestRouteTest
import akka.util.ByteString
import org.scalatest.{FunSpecLike, Matchers}
import weathergame.marshalling.WeatherServiceMarshaller
import weathergame.weather.Weather
import spray.json._
import weathergame.weather.WeatherTypes.{Hail, Precipitation, Rain, Snow}

import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

class OpenWeatherDeserializationSpec extends FunSpecLike with Matchers
  with WeatherServiceMarshaller with ScalatestRouteTest {

  val openWeatherJsonString = "{\"coord\":{\"lon\":-0.13,\"lat\":51.51},\"weather\":[{\"id\":500,\"main\":\"Rain\",\"description\":\"light rain\",\"icon\":\"10d\"}],\"base\":\"stations\",\"main\":{\"temp\":295.04,\"feels_like\":290.93,\"temp_min\":293.71,\"temp_max\":296.15,\"pressure\":1016,\"humidity\":40},\"visibility\":10000,\"wind\":{\"speed\":5.1,\"deg\":260},\"rain\":{\"1h\":0.15},\"clouds\":{\"all\":95},\"dt\":1592753883,\"sys\":{\"type\":1,\"id\":1414,\"country\":\"GB\",\"sunrise\":1592710990,\"sunset\":1592770891},\"timezone\":3600,\"id\":2643743,\"name\":\"London\",\"cod\":200}"

  val httpResponse = HttpResponse(entity = HttpEntity(contentType = ContentTypes.`application/json`,
    openWeatherJsonString))

  it("should properly deserialize open weather json payload into the Weather class") {
    val res: Future[JsValue] = httpResponse.entity.dataBytes.runFold(ByteString(""))(_ ++ _).map { body =>
      val json = body.decodeString(Charset.defaultCharset()).parseJson//.convertTo[Weather]
      json
      //body.decodeString(Charset.defaultCharset()).parseJson.convertTo[Weather]
    }
    val res0 = Await.result(res, Duration(10, "sec")).asInstanceOf[JsObject].fields
    val main = res0("main").asJsObject.fields //"main" is the key in the json from the http-response

    val temperature = Some(main.getOrElse("temp", None).asInstanceOf[JsNumber].value.toInt)

    val humidity = Some(main.getOrElse("humidity", None).asInstanceOf[JsNumber].value.toInt)

    val wind: Map[String, JsValue] = res0("wind").asJsObject.fields
    val windSpeed = Some(wind.getOrElse("speed", None).asInstanceOf[JsNumber].value.toInt)

    val precipitationDescription =
      res0("weather").asInstanceOf[JsArray]
      .elements.head.asInstanceOf[JsObject].fields
      .getOrElse("description", None).asInstanceOf[JsString].value
    val precipitation: Option[Precipitation] = precipitationDescription match {
      case s:String if s.toLowerCase.contains("rain") => Some(Rain())
      case s:String if s.toLowerCase.contains("snow") => Some(Snow())
      case s:String if s.toLowerCase.contains("hail") => Some(Hail())
      case _ => None
    }

    val location = Some(res0("name").asInstanceOf[JsString].value)

    val weatherRes = Weather(id = "000", temperature = temperature, precipitation = precipitation, humidity = humidity,
      wind = windSpeed, location = location)
    val weatherExpected = Weather(id = "000", temperature = Some(295), precipitation = Some(Rain()), humidity = Some(40),
      wind = Some(5), location = Some("London"))
    assert(weatherRes == weatherExpected)
  }


}

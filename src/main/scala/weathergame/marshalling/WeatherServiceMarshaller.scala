package weathergame.marshalling

import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json._
import weathergame.weather.WeatherList

trait WeatherServiceMarshaller extends SprayJsonSupport with DefaultJsonProtocol {
  import weathergame.gamemechanics.Result
  import weathergame.weather.WeatherTypes._
  import weathergame.weather.{ForecastRequest, Weather}
  import weathergame.player.{Player, Players}
  // formatters related to sky
  implicit val sunny = jsonFormat1(Sunny)
  implicit val cloudy = jsonFormat1(Cloudy)
  implicit val darkCloudy = jsonFormat1(DarkCloudy)
  implicit val partlyCloudy = jsonFormat1(PartlyCloudy)

  implicit val sky = new JsonFormat[Sky] {
    override def write(obj: Sky): JsValue = obj match {
      case sunnyImpl: Sunny => sunny.write(sunnyImpl)
      case cloudyImpl: Cloudy => cloudy.write(cloudyImpl)
      case darkCloudyImpl: DarkCloudy => darkCloudy.write(darkCloudyImpl)
      case partlyCloudyImpl: PartlyCloudy => partlyCloudy.write(partlyCloudyImpl)
    }

    override def read(json: JsValue): Sky = json.asJsObject.getFields("name") match {
      case Seq(JsString(name)) => name match {
        case "sunny" => Sunny()
        case "cloudy" => Cloudy()
        case "darkCloudy" => DarkCloudy()
        case "partlyCloudy" => PartlyCloudy()
        case _ => throw DeserializationException("Not a Sky type was provided")
      }
      case _ => throw DeserializationException("Object expected")
    }
  }

  // formatters related to precipitiation
  implicit val noPrecipitation = jsonFormat1(NoPrecipitation)
  implicit val rain = jsonFormat1(Rain)
  implicit val snow = jsonFormat1(Snow)
  implicit val hail = jsonFormat1(Hail)

  implicit val precipitation = new JsonFormat[Precipitation] {
    override def write(obj: Precipitation): JsValue = obj match {
      case noPrecipitationImpl: NoPrecipitation => noPrecipitation.write(noPrecipitationImpl)
      case rainImpl: Rain => rain.write(rainImpl)
      case snowImpl: Snow => snow.write(snowImpl)
      case hailImpl: Hail => hail.write(hailImpl)
    }

    override def read(json: JsValue): Precipitation = json.asJsObject.getFields("name") match {
      case Seq(JsString(name)) => name match {
        case "no" => NoPrecipitation()
        case "rain" => Rain()
        case "snow" => Snow()
        case "hail" => Hail()
        case _ => throw DeserializationException("Not a Precipitation type was provided")
      }
      case _ => throw DeserializationException("Object expected")
    }
  }
  // final Weather formatter
  implicit val weather = jsonFormat6(Weather)
  implicit val weatherList = jsonFormat1(WeatherList)

  implicit val forecastRequest = jsonFormat1(ForecastRequest)


  implicit val player = jsonFormat3(Player)
  implicit val players = jsonFormat1(Players)

  implicit val result = jsonFormat6(Result)

  case class Error(message: String)

  implicit val errorFormat = jsonFormat1(Error)
}


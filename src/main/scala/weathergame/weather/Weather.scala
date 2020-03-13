package weathergame.weather

import java.util.UUID.randomUUID

import WeatherUtils._
import weathergame.weather.WeatherTypes.{Precipitation, Sky, WeatherADT}

case class Weather(id: String = generateId,
                   temperature: Option[Int] = None,
                   precipitation: Option[Precipitation] = None,
                   sky: Option[Sky] = None,
                   humidity: Option[Int] = None,
                   wind: Option[Int] = None
                  )

case class WeatherList(list: Vector[Weather])

object WeatherTypes {

  sealed trait WeatherADT

  sealed trait Precipitation extends WeatherADT

  case class NoPrecipitation(name: String = "no") extends Precipitation

  case class Rain(name: String = "rain") extends Precipitation

  case class Snow(name: String = "snow") extends Precipitation

  case class Hail(name: String = "hail") extends Precipitation

  sealed trait Sky extends WeatherADT

  case class NoSky(name: String = "no") extends Sky

  case class Sunny(name: String = "sunny") extends Sky

  case class Cloudy(name: String = "cloudy") extends Sky

  case class DarkCloudy(name: String = "darkCloudy") extends Sky

  case class PartlyCloudy(name: String = "partlyCloudy") extends Sky

}

object WeatherUtils {
  def generateId = randomUUID.toString //todo how to use method

  def emptyWeather = Weather(id = "")
}




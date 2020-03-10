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

  case class Sunny(name: String = "sunny") extends Sky

  case class Cloudy(name: String = "cloudy") extends Sky

  case class DarkCloudy(name: String = "darkCloudy") extends Sky

  case class PartlyCloudy(name: String = "partlyCloudy") extends Sky

}

object WeatherUtils {
  def generateId = randomUUID.toString //todo how to use method

  def emptyWeather = Weather(id = "")

  def diff(weatherL :Weather, weatherR: Weather): WeatherDifference = {
    WeatherDifference(
      diffInts(weatherL.temperature, weatherR.temperature),
      diffWeatherADT(weatherL.precipitation, weatherR.precipitation),
      diffWeatherADT(weatherL.sky, weatherR.sky),
      diffInts(weatherL.humidity, weatherR.humidity),
      diffInts(weatherL.wind, weatherR.wind)
    )
  }

  private def diffInts(optionL: Option[Int], optionR: Option[Int]) = {
    if (optionL.isDefined && optionR.isDefined) {
      Some(optionL.get - optionR.get)
    } else None
  }

  private def diffWeatherADT[T <: WeatherADT](optionL: Option[T], optionR: Option[T]) = {
    if (optionL.isDefined && optionR.isDefined) {
      optionL.get == optionR.get
    } else false
  }
}




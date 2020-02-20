package weathergame.weather

import java.util.UUID.randomUUID

import Weather._

abstract case class Weather(val id: String = generateId,
                       val temperature: Option[Int] = None,
                       val precipitation: Option[Precipitation] = None,
                       val sky: Option[Sky] = None,
                       val humidity: Option[Int] = None,
                       val wind: Option[Int] = None
                      )

object Weather {
  def generateId = randomUUID.toString

  sealed trait Precipitation

  case class Rain() extends Precipitation
  case class Snow() extends Precipitation
  case class Hail() extends Precipitation

  sealed trait Sky

  case class Sunny() extends Sky
  case class Cloudy() extends Sky
  case class DarkCloudy() extends Sky
  case class PartlyCloudy() extends Sky
}




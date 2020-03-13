package weathergame.gamemechanics

import weathergame.weather.Weather
import weathergame.weather.WeatherTypes.{Precipitation, Sky}

trait DifferenceToResultMapper[U] {

  def toResult(u: U): Int

}

object DifferenceToResultMapperInstances {

  implicit val weatherDifferenceToResultMapper: DifferenceToResultMapper[Weather] =
    (weather: Weather) => temperatureDiffToScore(weather.temperature) + humidityDiffToScore(weather.humidity) +
    windDiffToScore(weather.wind) + precDiffToScore(weather.precipitation) + skyDiffToScore(weather.sky)

  private def temperatureDiffToScore(difference: Option[Int]) = difference match {
    case Some(diff) => if (diff.abs <= 10) {
      10 - diff.abs
    } else 0
    case None => 0
  }

  private def humidityDiffToScore(difference: Option[Int]) = difference match {
    case Some(diff) => if (diff.abs <= 30) {
      10 - diff.abs % 3
    } else 0
    case None => 0
  }

  private def windDiffToScore(difference: Option[Int]) = difference match {
    case Some(diff) => if (diff.abs <= 5) {
      10 - diff.abs * 2
    } else 0
    case None => 0
  }

  private def precDiffToScore(difference: Option[Precipitation]) = if (difference.isDefined) 5 else 0

  private def skyDiffToScore(difference: Option[Sky]) = if (difference.isDefined) 5 else 0

}


object DifferenceToResultMapper {

  object DifferenceToResultMapperSyntax {

    implicit class DifferenceToResultMapperOps[U](t: U) {
      implicit def toResult(implicit mapper: DifferenceToResultMapper[U]) = {
        mapper.toResult(t)
      }
    }

  }

  def toResult[U](u: U)(implicit mapper: DifferenceToResultMapper[U]) = {
    mapper.toResult(u)
  }

}

package weathergame.gamemechanics

import weathergame.weather.{Weather, WeatherDifference, WeatherUtils}

object ResultCalculator {

  type Result = Int

    def compare(forecast: Weather, reality: Weather) = {
      val difference: WeatherDifference = WeatherUtils.diff(forecast, reality)
      differenceToScore(difference)
    }

    private def differenceToScore(difference: WeatherDifference) = {
      temperatureDifferenceToScore(difference.temperatureDiff) +
        humidityDifferenceToScore(difference.humidityDiff) +
        windDifferenceToScore(difference.windDiff) +
        precipitationDifferenceToScore(difference.precipitationDiff) +
        skyDifferenceToScore(difference.skyDiff)
    }

    private def temperatureDifferenceToScore(difference: Option[Int]) = difference match {
      case Some(diff) => if (diff.abs <= 10) {
        10 - diff.abs
      } else 0
      case None => 0
    }

    private def humidityDifferenceToScore(difference: Option[Int]) = difference match {
      case Some(diff) => if (diff.abs <= 30) {
        10 - diff.abs % 3
      } else 0
      case None => 0
    }

    private def windDifferenceToScore(difference: Option[Int]) = difference match {
      case Some(diff) => if (diff.abs <= 5) {
        10 - diff.abs * 2
      } else 0
      case None => 0
    }

    private def precipitationDifferenceToScore(difference: Boolean) = {
      if (difference) 5 else 0
    }

    private def skyDifferenceToScore(difference: Boolean) = {
      if (difference) 5 else 0
    }

  // todo create instances of the Comparable type class
  /** this should be a difference between 2 temp-s, but one doesn't
    want to write a code sorta
    forecast.temperature match {
    case Some(temperatureForecast) => reality match {
    case Some(temperatureReality) => scala.math.abs(temperatureForecast - temperatureReality)
    case None => None
    }
    case None => None
    }
    but to use smth from cats instead, in order to write only temperatureForecast - temperatureReality
    and if smth is None, just return None without verbosity
     * */
}

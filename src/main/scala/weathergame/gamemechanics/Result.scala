package weathergame.gamemechanics

import weathergame.weather.Weather

case class Result(temperatureAccuracy: Double,
                  humidityAccuracy: Double,
                  windAccuracy: Double,
                  skyAccuracy: Int,
                  precipitationAccuracy: Int,
                  totalScore: Double) {
// todo create instances of the Comparable type class
  def compare(forecast: Weather, reality: Weather) = {
    val temperatureDiff = ???

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

  //case class Result(forecast: Weather, reality: Weather)

}

package weathergame.gamemechanics

import weathergame.weather.{Forecast, Reality}

case class Score(forecast: Forecast, reality: Reality) {

  def compare(forecast: Forecast, reality: Reality) = {
    val temperatureDiff = ??? /** this should be a difference between 2 temp-s, but one doesn't
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

}

package weathergame.weather

import weathergame.weather.WeatherTypes.WeatherADT

case class WeatherDifference(temperatureDiff: Option[Int] = None,
                             precipitationDiff: Option[WeatherADT] = None,
                             skyDiff: Option[WeatherADT] = None,
                             humidityDiff: Option[Int] = None,
                             windDiff: Option[Int] = None)

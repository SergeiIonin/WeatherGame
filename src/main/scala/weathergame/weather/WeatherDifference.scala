package weathergame.weather

case class WeatherDifference(temperatureDiff: Option[Int] = None,
                             precipitationDiff: Boolean = false,
                             skyDiff: Boolean = false,
                             humidityDiff: Option[Int] = None,
                             windDiff: Option[Int] = None)

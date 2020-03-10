package weathergame.weather

case class WeatherDifference(temperatureDiff: Option[Int] = None,
                             precipitationDiff: Boolean = false,
                             sky: Boolean = false,
                             humidity: Option[Int] = None,
                             wind: Option[Int] = None)

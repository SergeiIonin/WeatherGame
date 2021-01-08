package weathergame.openweather

import com.typesafe.config.ConfigFactory

trait OpenWeatherImpl extends WeatherApiProvider {
  val conf = ConfigFactory.load()
  val prefix = "weather-api.open-weather"
  val key = conf.getString(s"$prefix.key")
  val appUrl = conf.getString(s"$prefix.appUrl")
}

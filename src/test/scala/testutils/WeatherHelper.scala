package testutils

import weathergame.weather.Weather
import weathergame.weather.WeatherTypes.{Cloudy, NoPrecipitation, Rain, Snow, Sunny}

object WeatherHelper {
  val weather1 = Weather(id = "0", temperature = Some(27), precipitation = Some(Rain()),
    sky = Some(Sunny()), wind = Some(1), humidity = Some(70))

  val weather2 = Weather(id = "0", temperature = Some(27), precipitation = Some(Rain()),
    sky = Some(Sunny()), wind = Some(1), humidity = Some(70))

  val weather3 = Weather(id = "0", temperature = Some(2), precipitation = Some(Snow()),
    sky = Some(Sunny()), wind = Some(3), humidity = Some(75))

  val weather4 = Weather(id = "2", temperature = Some(3), precipitation = Some(Rain()),
    sky = Some(Cloudy()), wind = Some(4), humidity = Some(60))

  val weather5 = Weather(id = "0", temperature = Some(27), precipitation = Some(NoPrecipitation()),
    sky = Some(Sunny()), wind = Some(1), humidity = Some(70))

  // difference of similar Weather instances
  val weatherDiff1_2 = Weather(id = "0", temperature = Some(0), precipitation = Some(Rain()),
    sky = Some(Sunny()), wind = Some(0), humidity = Some(0))

  // difference of Weather instances with different precipitation field
  val weatherDiff1_3 = Weather(id = "0", temperature = Some(25), precipitation = None,
    sky = Some(Sunny()), wind = Some(2), humidity = Some(5))

  // difference of Weather instances with different precipitation field
  val weatherDiff1_4 = Weather(id = "", temperature = Some(24), precipitation = Some(Rain()),
    sky = None, wind = Some(3), humidity = Some(10))


}

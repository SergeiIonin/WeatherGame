package testutils

import weathergame.gamemechanics.ResultCalculator.Result
import weathergame.weather.Weather
import weathergame.weather.WeatherTypes.{Cloudy, NoPrecipitation, Rain, Snow, Sunny}

object WeatherHelper {
  // weather test samples
  val weather1 = Weather(id = "1", temperature = Some(27), precipitation = Some(Rain()),
    sky = Some(Sunny()), wind = Some(1), humidity = Some(70))
  val weather2 = Weather(id = "2", temperature = Some(27), precipitation = Some(Rain()),
    sky = Some(Sunny()), wind = Some(1), humidity = Some(70))
  val weather3 = Weather(id = "3", temperature = Some(2), precipitation = Some(Snow()),
    sky = Some(Sunny()), wind = Some(3), humidity = Some(75))
  val weather4 = Weather(id = "4", temperature = Some(3), precipitation = Some(Rain()),
    sky = Some(Cloudy()), wind = Some(4), humidity = Some(60))
  val weather5 = Weather(id = "5", temperature = Some(27), precipitation = Some(NoPrecipitation()),
    sky = Some(Sunny()), wind = Some(1), humidity = Some(70))
  val weatherWithTempNone = Weather(id = "6", temperature = None, precipitation = Some(NoPrecipitation()),
    sky = Some(Sunny()), wind = Some(1), humidity = Some(70))

  // difference of similar Weather instances
  val weatherDiff1_2 = Weather(id = "", temperature = Some(0), precipitation = Some(Rain()),
    sky = Some(Sunny()), wind = Some(0), humidity = Some(0))

  // difference of Weather instances with different precipitation field
  val weatherDiff1_3 = Weather(id = "", temperature = Some(25), precipitation = None,
    sky = Some(Sunny()), wind = Some(2), humidity = Some(5))

  // difference of Weather instances with different precipitation field
  val weatherDiff1_4 = Weather(id = "", temperature = Some(24), precipitation = Some(Rain()),
    sky = None, wind = Some(3), humidity = Some(10))

  // result samples
  val weatherDiffRes1_2 = Result("0", Some(40))
  val weatherDiffRes1_3 = Result("0", Some(20))

  val result1 = Result("1", Some(42))
  val result2 = Result("2", Some(21))

}

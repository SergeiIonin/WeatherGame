package weathergame.gamemechanics

import org.scalatest.FunSpecLike
import testutils.WeatherHelper

class DifferenceToResultMapperSpec extends FunSpecLike {

  import SubstractableInstances._
  import Substractable.SubstractableSyntax.SubstractableOps

  import DifferenceToResultMapperInstances._
  import DifferenceToResultMapper.DifferenceToResultMapperSyntax.DifferenceToResultMapperOps

  it("(1) should properly get Result when Forecast and RealWeather are similar") {
    val w1 = WeatherHelper.weather1
    val w2 = WeatherHelper.weather2

    val diff = w1 - w2
    assert(diff.toResult == WeatherHelper.weatherDiffRes1_2)
  }

  // todo add better description
  it("(2) should properly get Result when Forecast and RealWeather are different in precipitation field and the others") {
    val w1 = WeatherHelper.weather1
    val w3 = WeatherHelper.weather3

    val diff = w1 - w3
    assert(diff.toResult == WeatherHelper.weatherDiffRes1_3)
  }


}

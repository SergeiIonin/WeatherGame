package weathergame.gamemechanics

import org.scalatest.FunSpecLike
import testutils.WeatherHelper

class SubstractableSpec extends FunSpecLike {

  import SubstractableInstances._
  import Substractable.SubstractableSyntax.SubstractableOps

  it("(1) should properly substract one instance of Weather from the other which are similar") {
    val w1 = WeatherHelper.weather1
    val w2 = WeatherHelper.weather2

    val diff = w1 - w2
    assert(diff == WeatherHelper.weatherDiff1_2)
  }

  it("(2) should properly substract one instance of Weather from the other which are different in precipitation field") {
    val w1 = WeatherHelper.weather1
    val w3 = WeatherHelper.weather3

    val diff = w1 - w3
    assert(diff == WeatherHelper.weatherDiff1_3)
  }

  it("(3) should properly substract one instance of Weather from the other which are different in sky field") {
    val w1 = WeatherHelper.weather1
    val w4 = WeatherHelper.weather4

    val diff = w1 - w4
    assert(diff == WeatherHelper.weatherDiff1_4)
  }



}

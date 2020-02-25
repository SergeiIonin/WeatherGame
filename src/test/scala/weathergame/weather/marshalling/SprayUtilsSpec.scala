package weathergame.weather.marshalling

import org.scalatest.FunSpecLike
import spray.json.{JsString, JsValue}
import weathergame.weather.Weather
import weathergame.weather.WeatherTypes.{NoPrecipitation, Rain, Sunny}


class SprayUtilsSpec extends FunSpecLike {


  it("(1) should properly marshall weather object to json") {
    import weathergame.marshalling.SprayUtils.WeatherMarshaller._

    val weatherInst = Weather(id = "0", temperature = Some(27), precipitation = Some(NoPrecipitation()),
      sky = Some(Sunny()), wind = Some(1), humidity = Some(70))
    val weatherJson = weather.write(weatherInst)
    val expectedWeatherJson = "{\"humidity\":70,\"id\":\"0\",\"precipitation\":{\"name\":\"no\"},\"sky\":{\"name\":\"sunny\"},\"temperature\":27,\"wind\":1}"
    assert(expectedWeatherJson == weatherJson.toString())
  }

}

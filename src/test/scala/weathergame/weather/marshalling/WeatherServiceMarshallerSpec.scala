package weathergame.weather.marshalling

import org.scalatest.FunSpecLike
import weathergame.marshalling.WeatherServiceMarshaller
import weathergame.player.Player
import weathergame.weather.Weather
import weathergame.weather.WeatherTypes.{NoPrecipitation, Sunny}


class WeatherServiceMarshallerSpec extends FunSpecLike with WeatherServiceMarshaller {
  import spray.json._

  it("(1) should properly marshall weather object to json") {

    val weatherInst = Weather(id = "0", temperature = Some(27), precipitation = Some(NoPrecipitation()),
      sky = Some(Sunny()), wind = Some(1), humidity = Some(70))
    val weatherJson = weatherInst.toJson
    println(weatherJson.prettyPrint)
    val expectedWeatherJson = "{\"humidity\":70,\"id\":\"0\",\"precipitation\":{\"name\":\"no\"},\"sky\":{\"name\":\"sunny\"},\"temperature\":27,\"wind\":1}"
    assert(expectedWeatherJson == weatherJson.toString())
  }

  it("should properly marshall Player") {
  //  import spray.json._

    val playerImpl = Player(id = "0", login = "leo", description = "best ever")
    val playerJson = playerImpl.toJson.toString()
    val expectedPlayerJson = "{\"description\":\"best ever\",\"id\":\"0\",\"login\":\"leo\"}"
    assert(playerJson == expectedPlayerJson)
  }

}

package weathergame.weatherapis.client

import akka.http.scaladsl.testkit.{RouteTest, ScalatestRouteTest}
import org.scalatest.{FunSpec, FunSpecLike, Matchers, WordSpec}
import weathergame.marshalling.WeatherServiceMarshaller
import weathergame.openweather.WeatherWebClient

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class WeatherWebClientSpec extends FunSpecLike with WeatherServiceMarshaller
  with Matchers with ScalatestRouteTest {

  val client = new WeatherWebClient()
  val req = client.Request(location = Some("London,uk"))

  it("this test just run the request to OpenWeather") {
    val weather = Await.result(client.getWeather(req), Duration(10, "sec"))
    assert(1 == 1)
  }

}

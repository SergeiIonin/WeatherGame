package weathergame.calculator

import akka.actor.{Actor, ActorLogging, Props}
import weathergame.calculator.WeatherCalculatorActor.{Calculate, GetForecast}
import weathergame.weather.{Weather, WeatherUtils}

object WeatherCalculatorActor {

  def props(name: String) = Props(new WeatherCalculatorActor(name))

  /**
   *
   * */
  case class Calculate(forecast: Weather)
  case class GetRealWeather(reality: Weather) // will interact with openweather API
  case class GetForecast(id: String)

}

class WeatherCalculatorActor(name: String) extends Actor with ActorLogging {

  var forecasts = Map.empty[String, Weather]

  override def receive: Receive = {
    case Calculate(forecast) => {
      log.info(s"will add forecast $forecast")
      forecasts += (forecast.id -> forecast)
     /* val newResult = Result(89.0, 64.0, 90.0, 1, 2, 85.0) // stub
      results += (forecast.id -> newResult)*/
    }
    case GetForecast(id) => {
      log.info(s"sending forecast info to sender ${sender()}")
      sender() ! forecasts.getOrElse(id, WeatherUtils.emptyForecast)
    }
  }

}

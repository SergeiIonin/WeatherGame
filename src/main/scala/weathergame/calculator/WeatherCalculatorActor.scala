package weathergame.calculator

import akka.actor.{Actor, Props}
import weathergame.calculator.WeatherCalculatorActor.{Add, GetScore}
import weathergame.weather.{Forecast, RealWeather}

object WeatherCalculatorActor {

  def props(name: String) = Props(new WeatherCalculatorActor(name))

  /**
   * Add(forecast)
   * */

  case class Add(forecasts: List[Forecast])
  case class GetRealWeather(reality: List[RealWeather]) // will interact with openweather API
  case object GetScore

}

class WeatherCalculatorActor(name: String) extends Actor {

  var forecasts = List.empty[Forecast] // user can submit several forecasts

  override def receive: Receive = {
    case Add(forecasts) => forecasts ++ forecasts
    case GetScore =>

  }

}

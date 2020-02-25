package weathergame.calculator

import akka.actor.{Actor, Props}
import weathergame.calculator.WeatherCalculatorActor
import weathergame.calculator.WeatherCalculatorActor.Calculate
import weathergame.gamemechanics.Result
import weathergame.weather.Weather

object WeatherCalculatorActor {

  def props(name: String) = Props(new WeatherCalculatorActor(name))

  /**
   *
   * */
  case class Calculate(forecast: Weather)
  case class GetRealWeather(reality: Weather) // will interact with openweather API

}

class WeatherCalculatorActor(name: String) extends Actor {

  var forecasts = List.empty[Weather] // user can submit several forecasts
  var results = Map.empty[String, Result]

  override def receive: Receive = {
    case Calculate(forecast) => {
      val realWeather = Weather() // stub
      forecasts ++ List(forecast)
      val newResult = Result(89.0, 64.0, 90.0, 1, 2, 85.0) // stub
      results += (forecast.id -> newResult)

    }


  }

}

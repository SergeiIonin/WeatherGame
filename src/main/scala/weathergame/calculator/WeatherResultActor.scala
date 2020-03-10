package weathergame.calculator

import akka.actor.{Actor, ActorLogging, Props}
import weathergame.calculator.WeatherResultActor.GetRealWeatherAPI
import weathergame.weather.Weather
import weathergame.weather.WeatherTypes.{NoPrecipitation, Sunny}

object WeatherResultActor {

  def props(name: String) = Props(new WeatherResultActor(name))

  case class GetRealWeatherAPI(forecast: Weather)

}

class WeatherResultActor(name: String) extends Actor with ActorLogging {

  override def receive: Receive = {
    case GetRealWeatherAPI(forecast) => {
      log.info(s"will add forecast $forecast")
      /*sending request to OpenWeather API
      * and sending the RealWeather onComplete*/
      // fixme this is a stub!!!
      val newRealWeather = Weather(temperature = Some(27), precipitation = Some(NoPrecipitation()),
        sky = Some(Sunny()), wind = Some(1), humidity = Some(70))
      sender() ! WeatherCalculatorActor.AddRealWeather(newRealWeather, forecast.id)
    }
  }

}

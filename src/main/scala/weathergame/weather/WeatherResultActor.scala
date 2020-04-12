package weathergame.weather

import akka.actor.{Actor, ActorLogging, Props}
import weathergame.weather.WeatherResultActor.GetRealWeatherAPI
import weathergame.weather.WeatherTypes._

object WeatherResultActor {

  def props(name: String) = Props(new WeatherResultActor(name))

  case class GetRealWeatherAPI(login: String, forecastId: String)

}
/**
 * Gets raw weather by the external weather service API, packs it
 * into Weather and sends back to sender (WeatherCalculatorActor)
 * */
class WeatherResultActor(name: String) extends Actor with ActorLogging {

  override def receive: Receive = {
    case GetRealWeatherAPI(login, forecastId) => {
      log.info(s"will add forecast with id = $forecastId")
      /*sending request to OpenWeather API
      * and sending the RealWeather onComplete*/
      // fixme this is a stub!!!
      // todo get results from the external API and wrap it into Weather
      val newRealWeather = Weather(id = forecastId, temperature = Some(27), precipitation = Some(Rain()),
        sky = Some(Sunny()), wind = Some(1), humidity = Some(70))
      sender() ! WeatherActor.AddRealWeather(login, newRealWeather)
    }
  }

}

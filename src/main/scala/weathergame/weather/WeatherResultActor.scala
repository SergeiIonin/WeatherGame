package weathergame.weather

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.stream.ActorMaterializer
import weathergame.openweather.WeatherWebClient
import weathergame.weather.WeatherResultActor.GetRealWeatherByAPI
import weathergame.weather.WeatherTypes._

import scala.util.{Failure, Success}

object WeatherResultActor {

  def props(name: String) = Props(new WeatherResultActor(name))

  case class GetRealWeatherByAPI(login: String, forecast: Weather)

}
/**
 * Gets raw weather by the external weather service API, packs it
 * into Weather and sends back to sender (WeatherCalculatorActor)
 * */
class WeatherResultActor(name: String) extends Actor with ActorLogging {
  implicit val system = context.system
  implicit val exeContext = context.dispatcher
  implicit val mat = ActorMaterializer.apply()

  val client = new WeatherWebClient()

  override def receive: Receive = {
    case GetRealWeatherByAPI(login, forecast) => {
      val sndr: ActorRef = sender()
      log.info(s"will get real weather for id = ${forecast.id}, the location is ${forecast.location}," +
        s"sender is $sndr")
      /*sending request to OpenWeather API
      * and sending the real weather on complete*/
      val req = client.Request(forecast.id, forecast.date, forecast.location)
      client.getWeather(req) onComplete {
        case Success(realWeather) => {
          log.info(s"real weather received is $realWeather")
          sndr ! WeatherActor.AddRealWeather(login, realWeather)
        }
        case Failure(exc) => throw new Exception("can't get a response for the request" +
          s"${forecast.id} for location ${forecast.location} and date ${forecast.date}, " +
          s"exception thrown is ${exc.getMessage}")
      }
    }
  }

}

package weathergame.service

import akka.actor.{Actor, ActorRef, Props}
import akka.util.Timeout
import weathergame.calculator.WeatherCalculatorActor
import weathergame.player.{Player, PlayerActor, Players}
import weathergame.weather.Weather

import scala.concurrent.Future

object WeatherServiceActor {
  def props(implicit timeout: Timeout) = Props(new WeatherServiceActor)
  def name = "weatherService"

  // player protocol
  case class CreatePlayer(player: Player)
  case class GetPlayer(login: String)
  case object GetPlayers
  case class GetRaiting() // some improvements like pagination will be added later

  sealed trait PlayerResponse
  case class PlayerCreated(login: String) extends PlayerResponse
  case object PlayerExists extends PlayerResponse

  // weather protocol
  case class CreateForecast(forecast: Weather) // just one forecast could be created
  case class GetResult(forecastId: String)
  case class GetAllResults(playerId: String)

  sealed trait ForecastResponse
  case class ForecastCreated(name: String, forecast: Weather) extends ForecastResponse
  case object ForecastExists extends ForecastResponse

}

class WeatherServiceActor(implicit timeout: Timeout) extends Actor {
  import WeatherServiceActor._
  import context._

  def createWeatherCalculatorActor(name: String) =
    context.actorOf(WeatherCalculatorActor.props(name), name)

  def createPlayerActor(login: String) =
    context.actorOf(PlayerActor.props(login), login)

  var players = Set.empty[String]

  override def receive: Receive = {
    case CreatePlayer(player) => {
      val newLogin = player.login

      def create() = {
        if (!players.contains(newLogin)) {
          players += newLogin
          val playerActor = createPlayerActor(newLogin)
          playerActor ! PlayerActor.Add(player)
          sender() ! PlayerCreated(newLogin)
        } else
          sender() ! PlayerExists
      }

      create()
    }
    case GetPlayer(login) => {
      def notFound() = sender() ! None
      def getPlayer(child: ActorRef) = child forward PlayerActor.GetPlayer
      context.child(login).fold(notFound())(getPlayer)
    }
    case GetPlayers => {
      import akka.pattern.ask
      import akka.pattern.pipe

      def getPlayers = context.children.map { child =>
        self.ask(GetPlayer(child.path.name)).mapTo[Option[Player]]
      }
      def convertToPlayers(f: Future[Iterable[Option[Player]]]) =
        f.map(_.flatten).map(l => Players(l.toVector))

      pipe(convertToPlayers(Future.sequence(getPlayers))) to sender()
    }

    case CreateForecast(forecast) => {
      val name = forecast.id
      def create() = {
        val weatherCalculatorActor = createWeatherCalculatorActor(name)
        weatherCalculatorActor ! WeatherCalculatorActor.Calculate(forecast)
        sender() ! ForecastCreated(name, forecast)
      }
      create()
    }
  }
}

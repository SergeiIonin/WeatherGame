package weathergame.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import akka.pattern.ask
import weathergame.calculator.WeatherCalculatorActor
import weathergame.player.{Player, Players, PlayersActor}
import weathergame.weather.{Weather, WeatherList, WeatherUtils}

import scala.concurrent.Future
import scala.util.{Failure, Success}

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
  case class PlayerFailedToBeCreated(login: String) extends PlayerResponse

  // weather protocol
  case class CreateForecast(forecast: Weather) // just one forecast could be created
  case class GetForecast(forecastId: String)
  case object GetForecasts
  case class GetResult(forecastId: String)
  case class GetAllResults(playerId: String)

  sealed trait ForecastResponse
  case class ForecastCreated(forecastId: String, forecast: Weather) extends ForecastResponse
  case object ForecastExists extends ForecastResponse
  case class ForecastFailedToBeCreated(forecastId: String) extends ForecastResponse

}

class WeatherServiceActor(implicit timeout: Timeout) extends Actor with ActorLogging {
  import WeatherServiceActor._
  import context._

  def createWeatherCalculatorActor(name: String) =
    context.actorOf(WeatherCalculatorActor.props(name), name)

  def createPlayerActor(login: String) =
    context.actorOf(PlayersActor.props(login), login)

  var players = Set.empty[String]

  override def receive: Receive = {
    case CreatePlayer(player) => {
      val newLogin = player.login
      log.info(s"in WeatherServiceActor ready to create a player ${player.login}")
      def create() = {
        if (!players.contains(newLogin)) {
          players += newLogin
          val playerActor = createPlayerActor(newLogin)
          playerActor ! PlayersActor.Add(player)
          log.info(s"ready to send PlayerCreated to sender() ${sender()}")
          sender() ! PlayerCreated(newLogin)
          /*playerActor.ask(PlayerActor.Add(player)).mapTo[Player] onComplete {
            case Success(_) => {
              log.info(s"is about to send PlayerCreated of $newLogin to sender ${sender()}")
              sender() ! PlayerCreated(newLogin)
            }
            case _ => sender() ! PlayerFailedToBeCreated(newLogin)
          }*/
        } else
          sender() ! PlayerExists
      }

      create()
    }
    case GetPlayer(login) => {
      def notFound() = sender() ! None
      def getPlayer(child: ActorRef) = child forward PlayersActor.GetPlayer(login)

      context.child(login).fold(notFound())(getPlayer)
    }
    case GetPlayers => {
      import akka.pattern.ask
      import akka.pattern.pipe

      def getPlayers = context.children.map { child =>
        self.ask(GetPlayer(child.path.name)).mapTo[Player]
      }
      def convertToPlayers(f: Future[Iterable[Player]]) =
        f.map(l => Players(l.toVector))

      pipe(convertToPlayers(Future.sequence(getPlayers))) to sender()
    }
////
    case CreateForecast(forecast) => {
      val name = forecast.id
      def create() = {
        log.info(s"ready to create WeatherCalculatorActor $name")
        val weatherCalculatorActor = createWeatherCalculatorActor(name)
        weatherCalculatorActor ! WeatherCalculatorActor.Calculate(forecast)
        sender() ! ForecastCreated(name, forecast)
      }
      create()
    }
    case GetForecast(id) => {
      def notFound() = sender() ! None
      def getForecast(child: ActorRef) = child forward WeatherCalculatorActor.GetForecast(id)

      context.child(id).fold(notFound())(getForecast)
    }
    case GetForecasts => {
      import akka.pattern.ask
      import akka.pattern.pipe

      def getForecasts = context.children.map { child =>
        self.ask(GetForecast(child.path.name)).mapTo[Weather]
      }
      def convertToForecasts(f: Future[Iterable[Weather]]) =
        f.map(l => WeatherList(l.toVector))
      log.info(s"before piping to sender")
      //pipe(convertToForecasts(Future.sequence(getForecasts))) to sender()
      /*  val r = convertToForecasts(Future.sequence(getForecasts))
        val rOnComp = r onComplete {
          case Success(forecasts: WeatherList) => log.info(s"ready to pipe forecasts list $forecasts to sender")
          case Failure(_) => WeatherList(Vector(WeatherUtils.emptyForecast))
        }
        pipe(r) to sender()*/

      // fixme STUB!!!
      val forecasts = Future.apply(Iterable.single(WeatherUtils.emptyForecast))
      pipe(convertToForecasts(forecasts)) to sender()

      // fixme STUB!!!
      /*def notFound() = sender() ! None
      def getForecast(child: ActorRef) = child forward WeatherCalculatorActor.GetForecast("0")

      context.child("0").fold(notFound())(getForecast)*/

    }
  }
}

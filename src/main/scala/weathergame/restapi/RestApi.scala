package weathergame.restapi

import akka.actor._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.util.Timeout
import weathergame.gamemechanics.Result
import weathergame.marshalling.WeatherServiceMarshaller
import weathergame.player.{Player, Players}
import weathergame.service.PlayerServiceActor.{CreatePlayer, GetPlayer, GetPlayers, PlayerResponse}
import weathergame.service.{PlayerServiceActor, WeatherServiceActor}
import weathergame.weather.{Weather, WeatherList}

import scala.concurrent.ExecutionContext

class RestApi(system: ActorSystem, timeout: Timeout)
  extends RestRoutes {
  implicit val requestTimeout = timeout

  implicit def executionContext = system.dispatcher

  def createWeatherServiceActor = system.actorOf(WeatherServiceActor.props, WeatherServiceActor.name)
  def createPlayerServiceActor = system.actorOf(PlayerServiceActor.props, PlayerServiceActor.name)
}

trait RestRoutes extends WeatherServiceApi with WeatherServiceMarshaller {

  import StatusCodes._

  def routes: Route = playersRoute ~ playerRoute ~ forecastsRoute ~ forecastRoute //~ resultsRoute ~ forecastsRoute

  implicit val plr = jsonFormat3(Player)
  implicit val plrs = jsonFormat1(Players)
  implicit val wthr = jsonFormat6(Weather)
  implicit val wthrList = jsonFormat1(WeatherList)

  def playersRoute =
    pathPrefix("players") {
      pathEndOrSingleSlash {
        get {
          // GET /players
          onSuccess(getPlayers()) { players: Players =>
            complete(OK, players)
          }
        }
      }
    }

  def playerRoute =
    pathPrefix("players" / Segment) { login =>
      pathEndOrSingleSlash {
        post {
          // POST /players/:player
          entity(as[Player]) { player: Player =>
            onSuccess(createPlayer(player)) {
              case PlayerServiceActor.PlayerCreated(player.login) => complete(Created, player)
              case PlayerServiceActor.PlayerFailedToBeCreated(player.login) => complete(NoContent)
              case PlayerServiceActor.PlayerExists =>
                val err = Error(s"$login player already exists.")
                complete(BadRequest, err)
            }
          }
        } ~ get {
          // GET /players/:player
          onSuccess(getPlayer(login)) { player =>
            complete(OK, player)
          }
        }
      }
    }

  ////////////

  def forecastsRoute =
    pathPrefix("players" / Segment) { login => // does this pathPrefix required??
      path("forecasts") {
        pathEndOrSingleSlash {
          get {
            // GET /players/:player/forecasts
            onSuccess(getForecasts(login)) { forecasts =>
              complete(OK, forecasts)
            }
          }
        }
      }
    }

  def forecastRoute =
  pathPrefix("players" / Segment) { login =>
      path("forecasts" / Segment) { forecastId =>
        pathEndOrSingleSlash {
          post {
            // POST /players/:player/forecasts/:forecast
            entity(as[Weather]) { weather: Weather =>
              onSuccess(submitForecast(weather, login)) {
                case WeatherServiceActor.ForecastCreated(weather.id, weather) => complete(Created, weather)
                case WeatherServiceActor.ForecastFailedToBeCreated(weather.id) => complete(NoContent)
                case WeatherServiceActor.ForecastExists =>
                  val err = Error(s"${forecastId} player already exists.")
                  complete(BadRequest, err)
              }
            }
          } ~ get {
            // GET /players/:player/forecasts/:forecast
            onSuccess(getForecast(forecastId, login)) { forecast: Weather =>
              complete(OK, forecast)
            }
          }
        }
      }
    }


}

trait WeatherServiceApi {

  import weathergame.service.WeatherServiceActor._

  def createWeatherServiceActor(): ActorRef

  def createPlayerServiceActor(): ActorRef

  implicit def executionContext: ExecutionContext

  implicit def requestTimeout: Timeout

  lazy val weatherServiceActor = createWeatherServiceActor()
  lazy val playerServiceActor = createPlayerServiceActor()

  /*  def createGame(event: String) =
weatherServiceActor.ask(CreateEvent(event, nrOfTickets))
.mapTo[EventResponse]*/

  def createPlayer(player: Player) = {
    playerServiceActor.ask(CreatePlayer(player)).mapTo[PlayerResponse]
  }

  def getPlayer(login: String) = {
    playerServiceActor.ask(GetPlayer(login)).mapTo[Player]
  }

  def getPlayers() = {
    playerServiceActor.ask(GetPlayers).mapTo[Players]
  }

  ///////////////

  def submitForecast(weather: Weather, login: String) = {
    weatherServiceActor.ask(CreateForecast(weather, login)).mapTo[ForecastResponse] // was a headache when param weather was skipped in CreateForecast()
  }

  def getForecast(`forecast-id`: String, login: String) = {
    weatherServiceActor.ask(GetForecast(`forecast-id`, login)).mapTo[Weather]
  }

  def getForecasts(login: String) = {
    weatherServiceActor.ask(GetForecasts(login)).mapTo[WeatherList] // todo GetForecasts(login)
  }

  def getGamesResults(login: String) = {
    weatherServiceActor.ask(GetAllResults(login)).mapTo[Result]
  }

  /*  def getGamesResults() =
weatherServiceActor.ask(GetEvents).mapTo[Events]

def deleteGame(event: String) =
weatherServiceActor.ask(CancelEvent(event))
.mapTo[Option[Event]]

def getRaiting(event: String, tickets: Int) =
weatherServiceActor.ask(GetTickets(event, tickets))
.mapTo[TicketSeller.Tickets]*/
}

//

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
import weathergame.service.WeatherServiceActor
import weathergame.weather.{Weather, WeatherList}

import scala.concurrent.ExecutionContext

class RestApi(system: ActorSystem, timeout: Timeout)
  extends RestRoutes {
  implicit val requestTimeout = timeout

  implicit def executionContext = system.dispatcher

  def createWeatherServiceActor = system.actorOf(WeatherServiceActor.props, WeatherServiceActor.name)
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
              case WeatherServiceActor.PlayerCreated(player.login) => complete(Created, player)
              case WeatherServiceActor.PlayerFailedToBeCreated(player.login) => complete(NoContent)
              case WeatherServiceActor.PlayerExists =>
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

  /*// fixme TEMP
  def forecastsRoute =
    pathPrefix("forecasts") {
      pathEndOrSingleSlash {
        get {
          // GET /forecasts
          onSuccess(getForecasts()) { forecasts =>
            complete(OK, forecasts)
          }
        }
      }
    }

  def forecastRoute =
    pathPrefix("forecasts" / Segment) { forecastId =>
      pathEndOrSingleSlash {
        post {
          // POST /forecasts/:forecast
          entity(as[Weather]) { weather: Weather =>
            onSuccess(submitForecast(weather)) {
              case WeatherServiceActor.ForecastCreated(weather.id, weather) => complete(Created, weather)
              case WeatherServiceActor.ForecastFailedToBeCreated(weather.id) => complete(NoContent)
              case WeatherServiceActor.ForecastExists =>
                val err = Error(s"${forecastId} player already exists.")
                complete(BadRequest, err)
            }
          }
        } ~ get {
          // GET /forecasts/:forecast
          onSuccess(getForecast(forecastId)) { forecast: Weather =>
            complete(OK, forecast)
          }
        }
      }
    }*/

  // todo add this
  def forecastRoute =
    pathPrefix("players" / Segment) { _ =>
      path("forecasts" / Segment) { forecastId =>
        pathEndOrSingleSlash {
          post {
            // POST /players/:player/forecasts/:forecast
            entity(as[Weather]) { weather: Weather =>
              onSuccess(submitForecast(weather)) {
                case WeatherServiceActor.ForecastCreated(weather.id, weather) => complete(Created, weather)
                case WeatherServiceActor.ForecastFailedToBeCreated(weather.id) => complete(NoContent)
                case WeatherServiceActor.ForecastExists =>
                  val err = Error(s"${forecastId} player already exists.")
                  complete(BadRequest, err)
              }
            }
          } ~ get {
            // GET /players/:player/forecasts/:forecast
            onSuccess(getForecast(forecastId)) { forecast: Weather =>
              complete(OK, forecast)
            }
          }
        }
      }
    }

  def forecastsRoute =
    pathPrefix("players" / Segment) { _ =>
      path("forecasts") {
        pathEndOrSingleSlash {
          pathEndOrSingleSlash {
            get {
              // GET /players/:player/forecasts
              onSuccess(getForecasts()) { forecasts =>
                complete(OK, forecasts)
              }
            }
          }
        }
      }
    }

}

  trait WeatherServiceApi {

    import weathergame.service.WeatherServiceActor._

    def createWeatherServiceActor(): ActorRef

    implicit def executionContext: ExecutionContext

    implicit def requestTimeout: Timeout

    lazy val weatherServiceActor = createWeatherServiceActor()

    /*  def createGame(event: String) =
weatherServiceActor.ask(CreateEvent(event, nrOfTickets))
.mapTo[EventResponse]*/

    def createPlayer(player: Player) = {
      weatherServiceActor.ask(CreatePlayer(player)).mapTo[PlayerResponse]
    }

    def getPlayer(login: String) = {
      weatherServiceActor.ask(GetPlayer(login)).mapTo[Player]
    }

    def getPlayers() = {
      weatherServiceActor.ask(GetPlayers).mapTo[Players]
    }

    def submitForecast(weather: Weather) = {
      weatherServiceActor.ask(CreateForecast(weather)).mapTo[ForecastResponse] // was a headache when param weather was skipped
    }

    def getForecast(`forecast-id`: String) = {
      weatherServiceActor.ask(GetForecast(`forecast-id`)).mapTo[Weather]
    }

    def getForecasts() = {
      weatherServiceActor.ask(GetForecasts).mapTo[WeatherList]
    }

    def getGamesResults(playerId: String) = {
      weatherServiceActor.ask(GetAllResults(playerId)).mapTo[Result]
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

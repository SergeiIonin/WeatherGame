package weathergame.restapi

import akka.actor._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.util.Timeout
import weathergame.gamemechanics.Result
import weathergame.marshalling.WeatherServiceMarshaller
import weathergame.service.WeatherServiceActor
import weathergame.player.{Player, Players}
import weathergame.weather.Weather
import weathergame.weather.WeatherTypes.Rain

import scala.concurrent.ExecutionContext

class RestApi(system: ActorSystem, timeout: Timeout)
    extends RestRoutes {
  implicit val requestTimeout = timeout
  implicit def executionContext = system.dispatcher

  def createWeatherServiceActor = system.actorOf(WeatherServiceActor.props, WeatherServiceActor.name)
}

trait RestRoutes extends WeatherServiceApi with WeatherServiceMarshaller {

  import spray.json._
  import StatusCodes._

  def routes: Route = playersRoute ~ playerRoute //~ resultsRoute ~ forecastsRoute

  def playersRoute =
    pathPrefix("players") {
      pathEndOrSingleSlash {
        get {
          // GET /players
          onSuccess(getPlayers()) { players: Players =>
            complete(OK /*, players*/)
          }
        }
      }
    }

  implicit val plr = jsonFormat2(Player)

  def playerRoute =
    pathPrefix("players" / Segment) { player =>
      pathEndOrSingleSlash {
        post {
          // POST /players/:player
          entity(as[Player]) { p => complete(Created, p) /*plr: Player =>
            onSuccess(createPlayer(plr)) {
              case WeatherServiceActor.PlayerCreated(plr.login) => complete(Created, player)
              case WeatherServiceActor.PlayerExists =>
                val err = Error(s"$player player already exists.")
                complete(BadRequest, err)
            }*/
          }
        } /*~
          get {
            // GET /events/:event
            onSuccess(getPlayers()) {
              _.fold(complete(NotFound))(e => complete(OK, e))
            }
          } ~
          delete {
            // DELETE /events/:event
            onSuccess(cancelEvent(event)) {
              _.fold(complete(NotFound))(e => complete(OK, e))
            }
          }
      }
    }*/

        /*  def resultsRoute =
    pathPrefix("events") {
      pathEndOrSingleSlash {
        get {
          // GET /events
          onSuccess(getGamesResults()) { events =>
            complete(OK, events)
          }
        }
      }
    }*/

        /* def forecastsRoute =
    pathPrefix("forecasts" / Segment / "tickets") { event =>
      post {
        pathEndOrSingleSlash {
          // POST /events/:event/tickets
          entity(as[ForecastRequest]) { request =>
            onSuccess(submitForecast(request.weather)) {
              _ => complete(Created)
            }
          }
        }
      }
    }*/

      }
    }
}

trait WeatherServiceApi {

  import weathergame.service.WeatherServiceActor._

  def createWeatherServiceActor: ActorRef

  implicit def executionContext: ExecutionContext

  implicit def requestTimeout: Timeout

  lazy val weatherServiceActor = createWeatherServiceActor

  /*  def createGame(event: String) =
weatherServiceActor.ask(CreateEvent(event, nrOfTickets))
.mapTo[EventResponse]*/

  def createPlayer(player: Player) = {
    weatherServiceActor.ask(CreatePlayer(player)).mapTo[PlayerResponse]
  }

  def getPlayers() = {
    weatherServiceActor.ask(GetPlayers).mapTo[Players]
  }

  def submitForecast(weather: Weather) =
    weatherServiceActor.ask(CreateForecast).mapTo[Result]

  def getGamesResults(playerId: String) =
    weatherServiceActor.ask(GetAllResults(playerId)).mapTo[Result]

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

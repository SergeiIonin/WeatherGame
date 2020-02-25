package weathergame.restapi

import akka.actor._
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.util.Timeout
import spray.json.{DefaultJsonProtocol, RootJsonFormat}
import weathergame.gamemechanics.Result
import weathergame.marshalling.SprayUtils.{Error, ErrorJsonProtocol, ResultJsonProtocol, UserJsonProtocol, WeatherJsonProtocol}
import weathergame.service.WeatherServiceActor
import weathergame.weather.{User, Users, Weather}

import scala.concurrent.ExecutionContext

class RestApi(system: ActorSystem, timeout: Timeout)
    extends RestRoutes {
  implicit val requestTimeout = timeout
  implicit def executionContext = system.dispatcher

  def createWeatherServiceActor = system.actorOf(WeatherServiceActor.props, WeatherServiceActor.name)
}

trait RestRoutes extends WeatherServiceApi
with WeatherJsonProtocol with UserJsonProtocol with ResultJsonProtocol with ErrorJsonProtocol {

  import StatusCodes._
  import weathergame.marshalling.SprayUtils.UserMarshaller.{user, users}
  def routes: Route = usersRoute ~ userRoute //~ resultsRoute ~ forecastsRoute

  val x = implicitly[RootJsonFormat[User]]

  def usersRoute =
    pathPrefix("users") {
      pathEndOrSingleSlash {
        get {
          // GET /users
          onSuccess(getUsers()) { users: Users =>
            complete(OK/*, users*/)
          }
        }
      }
    }



  def userRoute =
    pathPrefix("games" / Segment) { user =>
      pathEndOrSingleSlash {
        post {
          // POST /games/:user
          entity(as[User]) { usr: User =>
            onSuccess(createUser(usr)) {
              case WeatherServiceActor.UserCreated(usr.login) => complete(Created, user)
              case WeatherServiceActor.UserExists =>
                val err = Error(s"$user user already exists.")
                complete(BadRequest, err)
            }
          }
        } /*~
          get {
            // GET /events/:event
            onSuccess(getUsers()) {
              _.fold(complete(NotFound))(e => complete(OK, e))
            }
          } ~
          delete {
            // DELETE /events/:event
            onSuccess(cancelEvent(event)) {
              _.fold(complete(NotFound))(e => complete(OK, e))
            }
          }*/
      }
    }

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

trait WeatherServiceApi {
  import weathergame.service.WeatherServiceActor._

  def createWeatherServiceActor: ActorRef
  implicit def executionContext: ExecutionContext
  implicit def requestTimeout: Timeout

  lazy val weatherServiceActor = createWeatherServiceActor

/*  def createGame(event: String) =
    weatherServiceActor.ask(CreateEvent(event, nrOfTickets))
      .mapTo[EventResponse]*/

  def createUser(user: User) = {
    weatherServiceActor.ask(CreateUser).mapTo[UserResponse]
  }

  def getUsers() = {
    weatherServiceActor.ask(GetUsers).mapTo[Users]
  }

  def submitForecast(weather: Weather) =
    weatherServiceActor.ask(CreateForecast).mapTo[Result]

  def getGamesResults(userId: String) =
    weatherServiceActor.ask(GetAllResults(userId)).mapTo[Result]

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

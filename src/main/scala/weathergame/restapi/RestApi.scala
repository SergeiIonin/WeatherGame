package weathergame.restapi

import akka.actor._
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server._
import akka.pattern.ask
import akka.util.Timeout
import weathergame.gamemechanics.ResultCalculator.Result
import weathergame.marshalling.WeatherServiceMarshaller
import weathergame.mongo.{MongoFactory, MongoService, MongoFactoryImpl}
import weathergame.player.{Player, Players}
import weathergame.service.PlayerServiceActor.{CreatePlayer, GetPlayer, GetPlayers, PlayerResponse}
import weathergame.service.{PlayerServiceActor, WeatherServiceActor}
import weathergame.weather.{Weather, WeatherList}

import scala.concurrent.ExecutionContext

class RestApi(system: ActorSystem, timeout: Timeout)
  extends RestRoutes {

  implicit val requestTimeout = timeout

  implicit def executionContext = system.dispatcher

  def createWeatherServiceActor = system.actorOf(WeatherServiceActor.props(MongoFactoryImpl), WeatherServiceActor.name)
  def createPlayerServiceActor = system.actorOf(PlayerServiceActor.props(MongoFactoryImpl), PlayerServiceActor.name)
}

trait RestRoutes extends WeatherServiceApi with WeatherServiceMarshaller {
  import StatusCodes._

  def routes: Route =
    playersRoute ~ playerRoute ~
      forecastsRoute ~ forecastRoute ~
      realWeathersRoute ~ realWeatherRoute
  //    resultsRoute ~ forecastsRoute

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
            // POST /players/:player/forecasts/:forecast-id
            entity(as[Weather]) { weather: Weather =>
              onSuccess(submitForecast(login, weather)) {
                case WeatherServiceActor.ForecastCreated(weather.id, weather) => complete(Created, weather)
                case WeatherServiceActor.ForecastFailedToBeCreated(weather.id) => complete(NoContent)
                case WeatherServiceActor.ForecastExists =>
                  val err = Error(s"$forecastId forecast already exists.")
                  complete(BadRequest, err)
              }
            }
          } ~ get {
            // GET /players/:player/forecasts/:forecast-id
            onSuccess(getForecast(login, forecastId)) { forecast: Weather =>
              complete(OK, forecast)
            }
          }
        }
      }
    }

  def realWeathersRoute =
    pathPrefix("players" / Segment) { login => // does this pathPrefix required??
      path("real-weathers") {
        pathEndOrSingleSlash {
          get {
            // GET /players/:player/realWeathers
            onSuccess(getRealWeathers(login)) { realWeathers =>
              complete(OK, realWeathers)
            }
          }
        }
      }
    }

  def realWeatherRoute =
    pathPrefix("players" / Segment) { login =>
      path("real-weathers" / Segment) { forecastId =>
        pathEndOrSingleSlash {
            get {
            // GET /players/:player/forecasts/:forecast-id
            onSuccess(getRealWeather(login, forecastId)) { realWeather: Weather =>
              complete(OK, realWeather)
            }
          }
        }
      }
    }

  def resultsRoute =
    pathPrefix("players" / Segment) { login => // does this pathPrefix required??
      path("results") {
        pathEndOrSingleSlash {
          get {
            // GET /players/:player/results
            onSuccess(getResults(login)) { results =>
              complete(OK, results)
            }
          }
        }
      }
    }

  def resultRoute =
  pathPrefix("players" / Segment) { login =>
      path("results" / Segment) { forecastId =>
        pathEndOrSingleSlash {
          get {
            // GET /players/:player/results/:forecast-id
            onSuccess(getResult(login, forecastId)) { result: Result =>
              complete(OK, result)
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

  // players
  def createPlayer(player: Player) = {
    playerServiceActor.ask(CreatePlayer(player)).mapTo[PlayerResponse]
  }

  def getPlayer(login: String) = {
    playerServiceActor.ask(GetPlayer(login)).mapTo[Player]
  }

  def getPlayers() = {
    playerServiceActor.ask(GetPlayers).mapTo[Players]
  }

  //forecasts
  def submitForecast(login: String, weather: Weather) = {
    weatherServiceActor.ask(CreateForecast(login, weather)).mapTo[ForecastResponse] // was a headache when param weather was skipped in CreateForecast()
  }

  def getForecast(login: String, `forecast-id`: String) = {
    weatherServiceActor.ask(GetForecast(login, `forecast-id`)).mapTo[Weather]
  }

  def getForecasts(login: String) = {
    weatherServiceActor.ask(GetForecasts(login)).mapTo[WeatherList]
  }

  //realWeathers
  def getRealWeather(login: String, `forecast-id`: String) = {
    weatherServiceActor.ask(GetRealWeather(login, `forecast-id`)).mapTo[Weather]
  }

  def getRealWeathers(login: String) = {
    weatherServiceActor.ask(GetRealWeathers(login)).mapTo[WeatherList]
  }

  //results
  def getResult(login: String, `forecast-id`: String) = {
    weatherServiceActor.ask(GetResult(login, `forecast-id`)).mapTo[Result]
  }

  def getResults(login: String) = {
    weatherServiceActor.ask(GetResults(login)).mapTo[Result]
  }

}


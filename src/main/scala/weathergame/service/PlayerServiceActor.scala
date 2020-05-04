package weathergame.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import weathergame.mongo.{MongoFactory, MongoService}
import weathergame.player.{Player, PlayerUtils, Players, PlayersActor}

import scala.concurrent.Future


object PlayerServiceActor {
  def props(factory: MongoFactory)(implicit timeout: Timeout) = Props(new PlayerServiceActor(factory))

  def name = "playerService"

  // player protocol
  case class CreatePlayer(player: Player)

  case class GetPlayer(login: String)

  case object GetPlayers

  case class GetRating() // some improvements like pagination will be added later

  sealed trait PlayerResponse

  case class PlayerCreated(login: String) extends PlayerResponse

  case object PlayerExists extends PlayerResponse

  case class PlayerFailedToBeCreated(login: String) extends PlayerResponse

}

class PlayerServiceActor(factory: MongoFactory)(implicit timeout: Timeout) extends Actor with ActorLogging
  with MongoService with MongoFactory {

  import PlayerServiceActor._
  import context._

  val mongoHost = factory.mongoHost
  val mongoPort = factory.mongoPort
  val databaseName = factory.databaseName
  val playersCollection = factory.playersCollection

  var playerActorsMap = Map.empty[String, ActorRef]

  def getPlayerActor(login: String) =
    context.child(login).getOrElse(context.actorOf(PlayersActor.props(login, factory), login))

  def playerNotFound() = sender() ! PlayerUtils.emptyPlayer

  var players = Vector.empty[String]

  override def preStart() = {
    super.preStart()
    players = mongoRepository.getAllPlayersLogins()
  }

  override def receive: Receive = {
    case CreatePlayer(player) => {
      val newLogin = player.login
      log.info(s"in PlayerServiceActor ready to create a player ${player.login}")

      def create() = {
        if (!players.contains(newLogin)) {
          players = players.appended(newLogin)
          val playerActor = getPlayerActor(newLogin)
          log.info(s"playerActor for $newLogin created ")
          playerActor ! PlayersActor.Add(player)
          log.info(s"ready to send PlayerCreated to sender() ${sender()}")
          sender() ! PlayerCreated(newLogin)
        } else
          sender() ! PlayerExists
      }

      create()
    }
    case GetPlayer(login) => {

      def getPlayer(child: ActorRef) = child forward PlayersActor.GetPlayer(login)

      players.find(_ == login) match {
        case Some(_) => getPlayer(getPlayerActor(login))
        case None => playerNotFound()
      }
    }
    case GetPlayers => {
      import akka.pattern.{ask, pipe}

      def getPlayers = {
        log.info(s"processing GetPlayers msg, player actors are ${context.children}")
        players.foreach(getPlayerActor)
        context.children.map { child =>
          self.ask(GetPlayer(child.path.name)).mapTo[Player]
        }
      }

      def convertToPlayers(f: Future[Iterable[Player]]) =
        f.map(l => Players(l.toVector))

      pipe(convertToPlayers(Future.sequence(getPlayers))) to sender()
    }
  }
}

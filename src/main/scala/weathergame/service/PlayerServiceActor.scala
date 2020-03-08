package weathergame.service

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.util.Timeout
import weathergame.player.{Player, Players, PlayersActor}

import scala.concurrent.Future


object PlayerServiceActor {
  def props(implicit timeout: Timeout) = Props(new PlayerServiceActor)

  def name = "playerService"

  // player protocol
  case class CreatePlayer(player: Player)

  case class GetPlayer(login: String)

  case object GetPlayers

  case class GetRaiting() // some improvements like pagination will be added later

  sealed trait PlayerResponse

  case class PlayerCreated(login: String) extends PlayerResponse

  case object PlayerExists extends PlayerResponse

  case class PlayerFailedToBeCreated(login: String) extends PlayerResponse

}


class PlayerServiceActor(implicit timeout: Timeout) extends Actor with ActorLogging {

  import PlayerServiceActor._
  import context._

  var playerActorsMap = Map.empty[String, ActorRef]

  def createPlayerActor(login: String) =
    context.actorOf(PlayersActor.props(login), login)

  var players = Set.empty[String]

  override def receive: Receive = {
    case CreatePlayer(player) => {
      val newLogin = player.login
      log.info(s"in PlayerServiceActor ready to create a player ${player.login}")

      def create() = {
        if (!players.contains(newLogin)) {
          players += newLogin
          val playerActor = createPlayerActor(newLogin)
          playerActorsMap += (newLogin -> playerActor) // fixme should be removed, not sure if it will always work
          playerActor ! PlayersActor.Add(player)
          log.info(s"ready to send PlayerCreated to sender() ${sender()}")
          sender() ! PlayerCreated(newLogin)
        } else
          sender() ! PlayerExists
      }

      create()
    }
    case GetPlayer(login) => {
      def notFound() = sender() ! None

      def getPlayer(child: ActorRef) = child forward PlayersActor.GetPlayer(login)

      context.child(login).fold(notFound())(getPlayer)
      //system.
      //playerActorsMap.get(login).fold(notFound())(getPlayer)
    }
    case GetPlayers => {
      import akka.pattern.{ask, pipe}

      def getPlayers = {
        log.info(s"processing GetPlayers msg, player actors are ${context.children}")
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

package weathergame.weather

import java.util.UUID.randomUUID
import UserUtils._

case class User(id: String = generateId,
                login: String
                  )

case class Users(users: Vector[User])

object UserUtils {
  def generateId = randomUUID.toString
}




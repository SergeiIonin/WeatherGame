package weathergame.mongo

import com.mongodb.MongoClient
import com.typesafe.config.ConfigFactory

object FakeMongoFactoryImpl extends MongoFactory {

  val conf = ConfigFactory.load()

  val mongoHost: String = "localhost"
  val mongoPort: String = "27017"
  val databaseName = "weathergame-fake"
  val playersCollection = "players-fake"

  val mongoClient: MongoClient = new MongoClient(mongoHost, mongoPort.toInt)

}

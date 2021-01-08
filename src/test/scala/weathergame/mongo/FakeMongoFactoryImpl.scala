package weathergame.mongo

object FakeMongoFactoryImpl extends MongoFactory {
  val mongoHost: String = "localhost"
  val mongoPort: String = "27017"
  val databaseName = "weathergame-fake"
  val playersCollection = "players-fake"
}

package weathergame.mongo

import com.typesafe.config.ConfigFactory

// for real mongo factory implementation
object MongoFactoryImpl extends MongoFactory {

  val conf = ConfigFactory.load()
  val mongoHost: String = conf.getString("mongo.host")
  val mongoPort: String = conf.getString("mongo.port")
  val databaseName = conf.getString("mongo.dbname")
  val playersCollection = conf.getString("mongo.players-collection")

}

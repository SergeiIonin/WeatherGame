package weathergame.mongo

import com.mongodb.client.{MongoCollection, MongoDatabase}
import com.mongodb.{ConnectionString, MongoClient}
import com.typesafe.config.ConfigFactory
import org.bson.Document


object MongoFactory {

  val conf = ConfigFactory.load()

  private val mongoHost: String = conf.getString("mongo.host")
  private val mongoPort: String = conf.getString("mongo.port")
  private val databaseName = conf.getString("mongo.dbname")
  private val playersCollection = conf.getString("mongo.players-collection")

  val mongoClient: MongoClient = new MongoClient(mongoHost, mongoPort.toInt)

  val db: MongoDatabase = mongoClient.getDatabase(databaseName)

  val collection: MongoCollection[Document] = db.getCollection(playersCollection)

}

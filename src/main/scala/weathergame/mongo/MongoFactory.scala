package weathergame.mongo

import com.mongodb.MongoClient
import com.mongodb.client.{MongoCollection, MongoDatabase}
import org.bson.Document

trait MongoFactory {

  val mongoHost: String
  val mongoPort: String
  val databaseName: String
  val playersCollection: String

  def mongoClient: MongoClient

  def db: MongoDatabase = mongoClient.getDatabase(databaseName)

  def collection: MongoCollection[Document] = db.getCollection(playersCollection)

}

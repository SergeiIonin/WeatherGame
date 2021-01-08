package weathergame.mongo

import com.mongodb.MongoClient
import com.mongodb.client.{MongoCollection, MongoDatabase}
import org.bson.Document

trait MongoFactory {

  def mongoHost: String

  def mongoPort: String

  def databaseName: String

  def playersCollection: String

  def mongoClient: MongoClient = new MongoClient(mongoHost, mongoPort.toInt)

  def db: MongoDatabase = mongoClient.getDatabase(databaseName)

  def collection: MongoCollection[Document] = db.getCollection(playersCollection)

}

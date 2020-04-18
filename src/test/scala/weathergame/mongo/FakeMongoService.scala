package weathergame.mongo

trait FakeMongoService extends MongoService {
  override val mongoFactory: MongoFactory = FakeMongoFactoryImpl
  override val mongoRepository = MongoRepository(mongoFactory)
}

package weathergame.mongo

trait MongoService {

  def mongoFactory: MongoFactory = MongoFactoryImpl
  def mongoRepository = MongoRepository(mongoFactory)

}

package weathergame.mongo

trait MongoService {
  factory: MongoFactory =>
  def mongoRepository = MongoRepository(factory)
}

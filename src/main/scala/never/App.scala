package never

import com.typesafe.config.ConfigFactory
import never.repository._
import never.ui.MainFrame

object App {
  private def loadDataFromPersistence(persistence: RepositoryPersistence, model: RepositoryModel, idGen: SequenceIdGenerator): Unit = {
    val events = persistence.readAllEvents
    model.processEvents(events)
    idGen.processEvents(events)
  }

  def main(args: Array[String]): Unit = {
    val appConfig = AppConfig(ConfigFactory.load())
    val model = new MemoryRepositoryModel()
    val persistence = new FileBasedRepositoryPersistence(appConfig)
    val idGen = new SimpleSequenceIdGenerator()
    loadDataFromPersistence(persistence, model, idGen)

    val writeApi: RepositoryWriteApi = new SimpleRepositoryWriteApi(idGen, List(model, persistence))
    val readApi: RepositoryReadApi = model

    new MainFrame(readApi, writeApi)
  }
}

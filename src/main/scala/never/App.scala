package never

import java.io.File

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
    val cfgFile = System.getProperty("never.cfgfile")
    val config = if(cfgFile != null) {
      ConfigFactory.parseFile(new File(cfgFile)).withFallback(ConfigFactory.load()).resolve()
    } else {
      ConfigFactory.load()
    }
    val appConfig = AppConfig(config)
    val model = new MemoryRepositoryModel()
    val persistence = new FileBasedRepositoryPersistence(appConfig)
    val idGen = new SimpleSequenceIdGenerator()
    loadDataFromPersistence(persistence, model, idGen)

    val writeApi: RepositoryWriteApi = new SimpleRepositoryWriteApi(idGen, List(model, persistence))
    val readApi: RepositoryReadApi = model

    new MainFrame(readApi, writeApi, appConfig)
  }
}

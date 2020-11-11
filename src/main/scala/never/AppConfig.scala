package never

import java.io.File

import com.typesafe.config.Config

object AppConfig {
  def apply(config: Config): AppConfig = {
    val neverConfig = config.getConfig("never")
    val dbFile = new File(neverConfig.getString("dbfile"))
    val maximizeOnStartup = neverConfig.getBoolean("maximize-on-startup")
    AppConfig(dbFile, maximizeOnStartup)
  }
}

case class AppConfig(dbFile: File, maximizeOnStartup: Boolean)

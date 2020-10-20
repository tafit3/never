package never

import java.io.File

import com.typesafe.config.Config

object AppConfig {
  def apply(config: Config): AppConfig = {
    AppConfig(new File(config.getString("dbfile")))
  }
}

case class AppConfig(dbFile: File)

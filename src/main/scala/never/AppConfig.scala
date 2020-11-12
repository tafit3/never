package never

import java.io.File

import com.typesafe.config.{Config, ConfigObject}
import never.domain.NodeMatchCondition
import never.domain.NodeMatchConditionConverter.toNodeMatchCondition

import scala.jdk.CollectionConverters._

object AppConfig {

  private def parseConfigValue[T](config: Config, path: String, parser: (Config, String) => T): Option[T] = {
    Option.when(config.hasPath(path))(parser(config, path))
  }

  private def parseFilteredViewCondition(config: Config): FilteredViewCondition = {
    val statusEq = parseConfigValue(config, "status-eq", _.getString(_))
    FilteredViewCondition(statusEq, Set.empty, Set.empty)
  }

  private def parseFilteredView(config: Config): FilteredView = {
    FilteredView(config.getString("name"),
      toNodeMatchCondition(config.getConfigList("conditions").asScala.map(parseFilteredViewCondition).toList))
  }

  private def parseFilteredViews(config: Config): List[FilteredView] = {
    config.getConfigList("filtered-views").asScala.map(parseFilteredView).toList
  }

  def apply(config: Config): AppConfig = {
    val neverConfig = config.getConfig("never")
    val dbFile = new File(neverConfig.getString("dbfile"))
    val maximizeOnStartup = neverConfig.getBoolean("maximize-on-startup")
    val filteredViews = parseFilteredViews(neverConfig)
    AppConfig(dbFile, maximizeOnStartup, filteredViews)
  }
}

case class AppConfig(dbFile: File, maximizeOnStartup: Boolean, filteredViews: List[FilteredView])

case class FilteredViewCondition(statusEqualTo: Option[String], containsAnyOfTags: Set[String], containsNoneOfTags: Set[String])

case class FilteredView(name: String, condition: NodeMatchCondition)
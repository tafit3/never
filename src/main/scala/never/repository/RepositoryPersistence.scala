package never.repository

import java.io._
import java.nio.charset.StandardCharsets.UTF_8

import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import never.AppConfig
import never.domain.NodeEvent
import org.apache.commons.io.FileUtils

import scala.jdk.CollectionConverters._

trait RepositoryPersistence extends EventsConsumer {
  def readAllEvents: List[NodeEvent]
}

class FileBasedRepositoryPersistence(appConfig: AppConfig) extends RepositoryPersistence {
  private val objectMapper = new ObjectMapper()
  objectMapper.registerModule(DefaultScalaModule)
  objectMapper.registerModule(new JavaTimeModule)
  objectMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
  private var writer: Option[BufferedWriter] = None

  override def readAllEvents: List[NodeEvent] = {
    if(appConfig.dbFile.exists()) {
      FileUtils.readLines(appConfig.dbFile, UTF_8).asScala.filterNot(_.trim().isBlank).map(deserialize).toList
    } else {
      Nil
    }
  }

  override def processEvents(events: List[NodeEvent]): Unit = {
    if(writer.isEmpty) {
      writer = Some(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(appConfig.dbFile, true))))
    }
    writer.foreach { wr =>
      events.map(serialize).foreach { line =>
        wr.write(line)
        wr.newLine()
      }
      wr.flush()
    }
  }

  private def serialize(event: NodeEvent): String = {
    val sw = new StringWriter
    objectMapper.writeValue(sw, event)
    val res = sw.toString
    res
  }

  private def deserialize(serializedEvent: String): NodeEvent = {
    objectMapper.readValue(serializedEvent, classOf[NodeEvent])
  }
}
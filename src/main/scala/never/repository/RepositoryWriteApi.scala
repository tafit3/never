package never.repository

import java.time.Instant

import never.domain._

trait RepositoryWriteApi {
  def addNode(status: String, content: String): Long
  def changeNodeContent(id: Long, content: String): Unit
  def changeNodeStatus(id: Long, status: String): Unit
  def moveNode(id: Long, parentInfo: Option[ParentInfo]): Unit
  def deleteNode(id: Long): Unit
}

class SimpleRepositoryWriteApi(idGen: SequenceIdGenerator, consumers: List[EventsConsumer]) extends RepositoryWriteApi {

  override def addNode(status: String, content: String): Long = {
    val nodeId = idGen.nextId()
    applyEvents(List(AddNode(nodeId, status, content)))
    nodeId
  }

  override def changeNodeContent(id: Long, content: String): Unit = {
    applyEvents(List(ChangeNodeContent(id, content)))
  }

  override def changeNodeStatus(id: Long, status: String): Unit = {
    applyEvents(List(ChangeNodeStatus(id, status)))
  }

  override def moveNode(id: Long, parentInfo: Option[ParentInfo]): Unit = {
    applyEvents(List(MoveNode(id, parentInfo)))
  }

  override def deleteNode(id: Long): Unit = {
    applyEvents(List(DeleteNode(id)))
  }

  private def applyEvents(eventsDetails: List[NodeEventDetails]): Unit = {
    val now = Instant.now()
    val events = eventsDetails.map(NodeEvent(now, _))
    consumers.foreach(_.processEvents(events))
  }
}
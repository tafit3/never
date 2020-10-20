package never.repository
import never.domain.{AddNode, NodeEvent}

trait SequenceIdGenerator extends EventsConsumer {
  def nextId(): Long
}

class SimpleSequenceIdGenerator extends SequenceIdGenerator {
  private var nextAvailableId: Long = 1

  override def nextId(): Long = {
    val id = nextAvailableId
    nextAvailableId += 1
    id
  }

  override def processEvents(events: List[NodeEvent]): Unit = {
    events.foreach { event =>
      event.details match {
        case AddNode(id, _, _) =>
          nextAvailableId = math.max(nextAvailableId, id + 1)
        case _ =>
      }
    }
  }
}

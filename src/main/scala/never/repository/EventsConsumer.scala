package never.repository

import never.domain.NodeEvent

trait EventsConsumer {
  def processEvents(events: List[NodeEvent]): Unit
}

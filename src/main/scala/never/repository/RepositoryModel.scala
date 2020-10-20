package never.repository

import never.domain.NodeView

trait RepositoryModel extends EventsConsumer with RepositoryReadApi {
  def nodeById(id: Long): Option[NodeView]
}
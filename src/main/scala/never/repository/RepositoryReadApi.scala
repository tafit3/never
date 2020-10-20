package never.repository

import never.domain.NodeView

trait RepositoryReadApi {
  def nodeById(id: Long): Option[NodeView]
  def allNodesByCreatedDesc: List[NodeView]
  def allNodesAsTreeByCreatedDesc(expandedNodes: Set[Long]): List[NodeView]
}

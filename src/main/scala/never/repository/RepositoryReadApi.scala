package never.repository

import never.domain.NodeView

trait RepositoryReadApi {
  def nodeById(id: Long): Option[NodeView]
  def allNodesByCreatedDesc(filter: Option[String]): List[NodeView]
  def allNodesAsTreeByCreatedDesc(filter: Option[String], expandedNodes: Set[Long]): List[NodeView]
}

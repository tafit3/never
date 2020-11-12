package never.repository

import never.domain.{NodeMatchCondition, NodeView}

case class NodesFilter(textSearchRegex: Option[String], nodeMatchCondition: NodeMatchCondition)

trait RepositoryReadApi {
  def nodeById(id: Long): Option[NodeView]
  def allNodesByCreatedDesc(nodesFilter: NodesFilter): List[NodeView]
  def allNodesAsTreeByCreatedDesc(nodesFilter: NodesFilter, expandedNodes: Set[Long]): List[NodeView]
}

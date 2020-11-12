package never.repository

import java.time.Instant
import java.util.regex.Pattern

import never.domain._
import never.util.CollectionUtils._

import scala.annotation.tailrec
import scala.collection.mutable

case class Node(id: Long, created: Instant, status: String, content: String, tags: Set[String], parent: Option[Long]) {
  require(id > 0, "id must be greater than 0")
}

case class ChildrenList(ids: List[Long]) {
  require(ids.nonEmpty, "children ids cannot be empty")
  require(ids.toSet.size == ids.size, s"children ids must be unique, actual: '$ids'")
}

class MemoryRepositoryModel extends RepositoryModel {
  private val nodeMatcher = new NodeMatcher()
  private val nodes = mutable.Map.empty[Long, Node]
  private val children = mutable.Map.empty[Long, ChildrenList]

  override def nodeById(id: Long): Option[NodeView] = {
    nodes.get(id).map(toNodeView(_))
  }

  override def allNodesByCreatedDesc(nodesFilter: NodesFilter): List[NodeView] = {
    withFilter(nodesFilter)(nodes.values.filter).toList.sortBy(_.created).reverse.map(toNodeView(_))
  }

  override def allNodesAsTreeByCreatedDesc(nodesFilter: NodesFilter, expandedNodes: Set[Long]): List[NodeView] = {
    nodes.values.filter(_.parent.isEmpty).toList.sortBy(_.created).reverse.flatMap { rootNode =>
      toFilteredNodeViewWithChildren(nodesFilter, rootNode, expandedNodes)
    }
  }

  private def toFilteredNodeViewWithChildren(nodesFilter: NodesFilter, node: Node, expandedNodes: Set[Long]): List[NodeView] = {
    val expandAllNodes = nodesFilter.textSearchRegex.isDefined
    withFilter(nodesFilter) { matcher =>
      def nodeMatches(nodeId: Long): Boolean = if(matcher(nodes(nodeId))) true else anyOfDescentantsMatches(nodeId)

      def anyOfDescentantsMatches(nodeId: Long): Boolean = children.get(nodeId).exists(_.ids.exists(nodeMatches))

      def visitDescendants(cur: Node, depth: Int = 0, parentId: Option[Long] = None): List[NodeView] = {
        val descendantsMatches = anyOfDescentantsMatches(cur.id)
        val expandable = !expandAllNodes && !expandedNodes(cur.id) && descendantsMatches
        val thisNode = toNodeView(cur, depth, parentId, expandable, matching = matcher(cur))
        val childrenNodes =
          if(expandAllNodes || descendantsMatches) {
            children.get(cur.id).map(_.ids.flatMap { childId =>
              visitDescendants(nodes(childId), depth + 1, Some(cur.id))
            }).toList.flatten
          } else {
            Nil
          }
        if (thisNode.matching || childrenNodes.nonEmpty) {
          thisNode +: (if(expandAllNodes || expandedNodes(cur.id)) childrenNodes else Nil)
        } else {
          Nil
        }
      }
      visitDescendants(node)
    }
  }

  private def withFilter[T](nodesFilter: NodesFilter)(f: (Node => Boolean) => T): T = {
    val textSearchPattern = nodesFilter.textSearchRegex.map(Pattern.compile(_, Pattern.CASE_INSENSITIVE))
    f { node =>
      textSearchPattern.forall {
        pattern => pattern.matcher(node.content).find() || node.tags.exists(pattern.matcher(_).find())
      } && nodeMatcher.matches(node, nodesFilter.nodeMatchCondition)
    }
  }

  private def toNodeView(node: Node, depth: Int = 0, parentId: Option[Long] = None, expandable: Boolean = false, matching: Boolean = false): NodeView = {
    NodeView(node.id, node.created, node.status, node.content, node.tags, depth, parentId, expandable, matching)
  }

  override def processEvents(events: List[NodeEvent]): Unit = {
    events.foreach(saveEvent)
  }

  private def saveEvent(event: NodeEvent): Unit = {
    event.details match {
      case AddNode(id, status, content) =>
        nodes += id -> Node(id, event.created, status, content, Set.empty, None)
      case ChangeNodeStatus(id, status) =>
        require(nodes.contains(id))
        nodes += id -> nodes(id).copy(status = status)
      case ChangeNodeContent(id, content) =>
        require(nodes.contains(id))
        nodes += id -> nodes(id).copy(content = content)
      case DeleteNode(id) =>
        require(nodes.contains(id))
        nodes.remove(id)
      case MoveNode(id, parentInfo) =>
        moveNode(id, parentInfo)
      case SetTags(id, tags) =>
        setTags(id, tags)
    }
  }

  private def setTags(id: Long, tags: Set[String]): Unit = {
    require(nodes.contains(id), s"Node not found. id='$id'")
    nodes += id -> nodes(id).copy(tags = tags)
  }

  private def moveNode(id: Long, parentInfo: Option[ParentInfo]): Unit = {
    require(nodes.contains(id), s"Node not found. id='$id'")
    unlink(id)
    parentInfo.foreach(link(id, _))
  }

  private def link(id: Long, parent: ParentInfo): Unit = {
    require(nodes.contains(parent.id), s"Parent node not found in attempt to link id='$id' to parent='$parent'.")
    require(id != parent.id, "Cannot link id with itself")
    require(!isAncestor(id, parent.id), "An attempt to turn the ancestor node into a child of this node.")
    children += parent.id -> ChildrenList(children.get(parent.id).map(_.ids).getOrElse(Nil).insert(parent.childIndex, id))
    nodes += id -> nodes(id).copy(parent = Some(parent.id))
  }

  @tailrec
  private def isAncestor(idToCheck: Long, refId: Long): Boolean = {
    if(idToCheck == refId) {
      true
    } else {
      nodes(refId).parent match {
        case Some(parent) => isAncestor(idToCheck, parent)
        case None => false
      }
    }
  }

  private def unlink(id: Long): Unit = {
    val node = nodes(id)
    node.parent.foreach { parent =>
      val existingChildren = children(parent).ids
      if(existingChildren.size == 1) {
        require(existingChildren.head == id, s"Children='$existingChildren' do not contain expected node id='$id'.")
        children.remove(parent)
      } else {
        val index = existingChildren.indexOf(id)
        require(index >= 0, s"An attempt to remove non-existent node id='$id' from children='$existingChildren'.")
        children += parent -> ChildrenList(existingChildren.remove(index))
      }
    }
    nodes += id -> node.copy(parent = None)
  }
}

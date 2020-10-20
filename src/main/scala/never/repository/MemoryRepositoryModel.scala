package never.repository

import never.util.CollectionUtils._
import java.time.Instant

import never.domain.{AddNode, ChangeNodeContent, ChangeNodeStatus, MoveNode, NodeEvent, NodeView, ParentInfo, DeleteNode}

import scala.annotation.tailrec
import scala.collection.mutable

case class Node(id: Long, created: Instant, status: String, content: String, parent: Option[Long]) {
  require(id > 0, "id must be greater than 0")
}

case class ChildrenList(ids: List[Long]) {
  require(ids.nonEmpty, "children ids cannot be empty")
  require(ids.toSet.size == ids.size, s"children ids must be unique, actual: '$ids'")
}

class MemoryRepositoryModel extends RepositoryModel {
  private val nodes = mutable.Map.empty[Long, Node]
  private val children = mutable.Map.empty[Long, ChildrenList]

  override def nodeById(id: Long): Option[NodeView] = {
    nodes.get(id).map(toNodeView(_))
  }

  override def allNodesByCreatedDesc: List[NodeView] = {
    nodes.values.toList.sortBy(_.created).reverse.map(toNodeView(_))
  }

  override def allNodesAsTreeByCreatedDesc(expandedNodes: Set[Long]): List[NodeView] = {
    nodes.values.filter(_.parent.isEmpty).toList.sortBy(_.created).reverse.flatMap { rootNode =>
      toNodeViewWithChildren(rootNode, expandedNodes)
    }
  }

  private def toNodeViewWithChildren(node: Node, expandedNodes: Set[Long]): List[NodeView] = {
    def visitDescendants(cur: Node, depth: Int, parentId: Option[Long]): List[NodeView] = {
      if(expandedNodes.contains(cur.id)) {
        toNodeView(cur, depth, parentId) +: children.get(cur.id).map(_.ids.flatMap { childId =>
          visitDescendants(nodes(childId), depth + 1, Some(cur.id))
        }).toList.flatten
      } else {
        List(toNodeView(cur, depth, parentId, children.contains(cur.id)))
      }
    }
    visitDescendants(node, 0, None)
  }

  private def toNodeView(node: Node, depth: Int = 0, parentId: Option[Long] = None, expandable: Boolean = false): NodeView = {
    NodeView(node.id, node.created, node.status, node.content, depth, parentId, expandable)
  }

  override def processEvents(events: List[NodeEvent]): Unit = {
    events.foreach(saveEvent)
  }

  private def saveEvent(event: NodeEvent): Unit = {
    event.details match {
      case AddNode(id, status, content) =>
        nodes += id -> Node(id, event.created, status, content, None)
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
    }
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

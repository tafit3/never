package never.ui

import never.domain.NodeView
import never.util.Constants.EMPTY_VALUE_PLACEHOLDER
import never.util.DateUtils

trait TaskListModel {
  def setStateAccessor(stateAccessor: TaskListAreaAccessor)
  def setNodes(nodes: List[NodeView]): Unit
  def selectedNode: Option[NodeView]
  def requestFocus(): Unit
  def flipTimestampVisibility(): Unit
}

class TaskListModelImpl extends TaskListModel with StateAccessorSupport[TaskListAreaAccessor] {
  private var nodes: List[NodeView] = Nil
  private var timestampsVisible = false

  def setNodes(nodes: List[NodeView]): Unit = {
    val selected = selectedNode
    this.nodes = nodes
    val selectedIndex = indexOf(selected.map(_.id))
    viewThem(selectedIndex)
  }

  private def indexOf(nodeIdMaybe: Option[Long]): Int = {
    nodeIdMaybe.flatMap(nodeId => nodes.zipWithIndex.collectFirst {
      case (node, index) if node.id == nodeId => index
    }).map(_ + 1).getOrElse(0)
  }

  def selectedNode: Option[NodeView] = {
    val line = stateAccessor.getLineOfOffset(stateAccessor.getCaretPosition)
    Option.when(line >= 1 && line <= nodes.size)(nodes(line-1))
  }

  def flipTimestampVisibility(): Unit = {
    timestampsVisible = !timestampsVisible
    viewThem(indexOf(selectedNode.map(_.id)))
  }

  private def viewThem(selectedIndex: Int): Unit = {
    require(selectedIndex >= 0 && selectedIndex <= nodes.length, s"selectedIndex='$selectedIndex' out of bound. nodes.length='${nodes.length}'")
    stateAccessor.setText(EMPTY_VALUE_PLACEHOLDER+"\n"+nodes.map(nodeToLine).mkString("\n"), selectedIndex)
  }

  private def nodeToLine(node: NodeView): String = {
    val date = if(timestampsVisible) DateUtils.format(node.created) + " " else ""
    ("." * node.depth) + date + node.status + " " + flatPrefix(node.content, 50) + (if(node.expandable) " ..." else "")
  }

  private def flatPrefix(s: String, maxLen: Int): String = {
    val flat = s.replaceAll("\\s+", " ").trim()
    if(flat.length > maxLen) {
      flat.substring(0, maxLen)
    } else {
      flat
    }
  }

  def requestFocus(): Unit = {
    stateAccessor.requestFocus()
  }
}
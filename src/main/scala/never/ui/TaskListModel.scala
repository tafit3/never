package never.ui

import never.domain.NodeView
import never.util.Constants.EmptyValuePlaceholder
import never.util.DateUtils

trait TaskListModel {
  def setStateAccessor(stateAccessor: TaskListAreaAccessor)
  def setNodes(nodes: List[NodeView]): Unit
  def selectedNode: Option[NodeView]
  def requestFocus(): Unit
  def flipTimestampVisibility(): Unit
  def isFocused(): Boolean
}

class TaskListModelImpl extends TaskListModel with StateAccessorSupport[TaskListAreaAccessor] {
  private val TwoNewLines = """\n\s*\n""".r
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
    val line = stateAccessor.getLineOfCaretPosition
    Option.when(line >= 1 && line <= nodes.size)(nodes(line-1))
  }

  def flipTimestampVisibility(): Unit = {
    timestampsVisible = !timestampsVisible
    viewThem(indexOf(selectedNode.map(_.id)))
  }

  private def viewThem(selectedIndex: Int): Unit = {
    require(selectedIndex >= 0 && selectedIndex <= nodes.length, s"selectedIndex='$selectedIndex' out of bound. nodes.length='${nodes.length}'")
    val nodesSerialized = nodes.map(nodeToLine).mkString("\n")
    stateAccessor.setText(EmptyValuePlaceholder+(if(nodes.nonEmpty) "\n"+nodesSerialized else ""), selectedIndex)
  }

  private def nodeToLine(node: NodeView): String = {
    val date = if(timestampsVisible) DateUtils.format(node.created) + " " else ""
    val tags = if(node.tags.nonEmpty) {
      node.tags.mkString("[", ", ", "] ")
    } else ""
    ("." * (node.depth+1)) +(if(node.expandable) "*" else " ")+ " " + date + node.status + " " + tags + flatPrefix(node.content, 90)
  }

  private def flatPrefix(s: String, maxLen: Int): String = {
    val twoNewLinesMatcher = TwoNewLines.pattern.matcher(s)
    val processed = if(twoNewLinesMatcher.find()) {
      s.substring(0, twoNewLinesMatcher.start())
    } else {
      s
    }
    val flat = processed.replaceAll("\\s+", " ").trim()
    if(flat.length > maxLen) {
      val i = flat.lastIndexOf(' ', maxLen)
      flat.substring(0, if (i >= 0) i else maxLen).trim() + "..."
    } else {
      flat
    }
  }

  def requestFocus(): Unit = {
    stateAccessor.requestFocus()
  }

  override def isFocused(): Boolean = {
    stateAccessor.isFocused()
  }

}
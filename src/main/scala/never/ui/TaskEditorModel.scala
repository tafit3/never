package never.ui

import never.domain.NodeView
import never.repository.{RepositoryReadApi, RepositoryWriteApi}
import never.ui.TaskEditorModel.{AddingNewNode, EditingExistingNode, EditingState, Empty}

object TaskEditorModel {
  sealed trait EditingState
  case object Empty extends EditingState
  case object AddingNewNode extends EditingState
  case object EditingExistingNode extends EditingState
}

trait TaskEditorModel {
  def editNewNode(): Unit
  def setStateAccessor(stateAccessor: TaskEditorAreaAccessor): Unit
  def loseFocus(): Unit
  def editNode(id: Long, focusEditor: Boolean): Unit
  def addListener(listener: TaskEditorListener): Unit
  def editingNodeId: Option[Long]
  def save(): Unit
}

class TaskEditorModelImpl(readApi: RepositoryReadApi, writeApi: RepositoryWriteApi)
  extends TaskEditorModel with ListenerSupport[TaskEditorListener] with StateAccessorSupport[TaskEditorAreaAccessor] {
  private var editingNode: Option[NodeView] = None
  private var editingState: EditingState = Empty

  def save(): Unit = {
    editingState match {
      case Empty =>
      case AddingNewNode =>
        if(!stateAccessor.content.isBlank) {
          finishEdit()
        }
      case EditingExistingNode =>
        require(editingNode.isDefined)
        if(editingNode.exists(node => (node.content != stateAccessor.content) || (node.tags != stateAccessor.tags))) {
          finishEdit()
        }
    }
  }

  private def finishEdit(): Unit = {
    val id = editingNode match {
      case Some(node) =>
        if(node.content != stateAccessor.content) {
          writeApi.changeNodeContent(node.id, stateAccessor.content)
        }
        if(node.tags != stateAccessor.tags) {
          writeApi.setTags(node.id, stateAccessor.tags)
        }
        node.id
      case None =>
        val nodeId = writeApi.addNode("TODO", stateAccessor.content)
        if(stateAccessor.tags.nonEmpty) {
          writeApi.setTags(nodeId, stateAccessor.tags)
        }
        nodeId
    }
    editingState = Empty
    setEditingNode(readApi.nodeById(id))
    fire(_.nodeSaved(id))
  }

  def editingNodeId: Option[Long] = {
    editingNode.map(_.id)
  }

  def editNode(id: Long, focusEditor: Boolean): Unit = {
    if(editingNode.forall(_.id != id)) {
      readApi.nodeById(id).foreach { node =>
        save()
        setEditingNode(Some(node))
      }
    }
    if(focusEditor) {
      stateAccessor.requestFocus()
    }
  }

  private def setEditingNode(node: Option[NodeView]): Unit = {
    editingNode = node
    editingNode.foreach(_ => editingState = EditingExistingNode)
    fire(_.editingNodeChanged(editingState, node))
  }

  def setEmptyEditingState(): Unit = {
    editingState = Empty
    setEditingNode(None)
  }

  def loseFocus(): Unit = {
    fire(_.loseFocus())
  }

  def editNewNode(): Unit = {
    save()
    editingState = AddingNewNode
    setEditingNode(None)
    stateAccessor.requestFocus()
  }
}

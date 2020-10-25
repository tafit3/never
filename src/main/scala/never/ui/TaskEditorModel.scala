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
  def editNode(id: Long): Unit
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
        if(!stateAccessor.getText.isBlank) {
          finishEdit()
        }
      case EditingExistingNode =>
        require(editingNode.isDefined)
        if(editingNode.exists(_.content != stateAccessor.getText)) {
          finishEdit()
        }
    }
  }

  private def finishEdit(): Unit = {
    val id = editingNode match {
      case Some(node) =>
        writeApi.changeNodeContent(node.id, stateAccessor.getText)
        node.id
      case None =>
        writeApi.addNode("TODO", stateAccessor.getText)
    }
    editingState = Empty
    setEditingNode(readApi.nodeById(id))
    fire(_.nodeSaved(id))
  }

  def editingNodeId: Option[Long] = {
    editingNode.map(_.id)
  }

  override def editNode(id: Long): Unit = {
    if(editingNode.forall(_.id != id)) {
      readApi.nodeById(id).foreach { node =>
        save()
        setEditingNode(Some(node))
        stateAccessor.requestFocus()
      }
    }
  }

  private def setEditingNode(node: Option[NodeView]): Unit = {
    editingNode = node
    editingNode.foreach(_ => editingState = EditingExistingNode)
    fire(_.editingNodeChanged(editingState, node))
  }

  def clear(): Unit = {
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

package never.ui

import never.domain.NodeView
import never.repository.{RepositoryReadApi, RepositoryWriteApi}
import never.ui.TaskEditorModel.{AddingNewNode, EditingExistingNode, EditingState, Empty}

object TaskEditorModel {
  sealed trait EditingState
  case object Empty extends EditingState
  case class AddingNewNode(newNodeStatus: String) extends EditingState
  case object EditingExistingNode extends EditingState
}

trait TaskEditorModel {
  def editNewNode(newNodeStatus: Option[String]): Unit
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
      case AddingNewNode(newNodeStatus) =>
        if(!stateAccessor.content.isBlank) {
          val nodeId = writeApi.addNode(newNodeStatus, stateAccessor.content)
          if(stateAccessor.tags.nonEmpty) {
            writeApi.setTags(nodeId, stateAccessor.tags)
          }
          finishEdit(nodeId)
        }
      case EditingExistingNode =>
        require(editingNode.isDefined)
        editingNode.foreach { node =>
          if((node.content != stateAccessor.content) || (node.tags != stateAccessor.tags)) {
            if (node.content != stateAccessor.content) {
              writeApi.changeNodeContent(node.id, stateAccessor.content)
            }
            if (node.tags != stateAccessor.tags) {
              writeApi.setTags(node.id, stateAccessor.tags)
            }
            finishEdit(node.id)
          }
        }
    }
  }

  private def finishEdit(id: Long): Unit = {
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

  def editNewNode(newNodeStatus: Option[String]): Unit = {
    save()
    editingState = AddingNewNode(newNodeStatus.getOrElse("TODO"))
    setEditingNode(None)
    stateAccessor.requestFocus()
  }
}

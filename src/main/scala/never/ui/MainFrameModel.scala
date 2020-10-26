package never.ui

import never.domain.{NodeView, ParentInfo}
import never.repository.{RepositoryReadApi, RepositoryWriteApi}

import scala.collection.mutable

object MainFrameModel {

  sealed trait ViewType {
    def name: String
  }
  case object AllFlatByTimeDesc extends ViewType {
    val name = "all tasks flat sorted by time descending"
  }
  case object AllTreeByTimeDesc extends ViewType {
    val name = "all tasks in tree sorted by time descending"
  }
  val AllViewTypes = List(AllFlatByTimeDesc, AllTreeByTimeDesc)
}

trait MainFrameModel {
  def editSelectedNode(): Unit
}

class MainFrameModelImpl(taskListModel: TaskListModel,
                         taskEditorModel: TaskEditorModel,
                         readApi: RepositoryReadApi,
                         writeApi: RepositoryWriteApi) extends MainFrameModel {
  import MainFrameModel._
  private var viewType: ViewType = AllTreeByTimeDesc
  private val expandedNodes = mutable.Set.empty[Long]
  private var selectedForMove: Option[Long] = None

  def refreshTaskList(): Unit = {
    taskListModel.setNodes(readNodes())
  }

  taskEditorModel.addListener(new TaskEditorListener {
    def nodeSaved(id: Long): Unit = {
      refreshTaskList()
    }

    override def loseFocus(): Unit = {
      taskListModel.requestFocus()
    }

    override def editingNodeChanged(editingState: TaskEditorModel.EditingState, editingNode: Option[NodeView]): Unit = ()
  })

  private def readNodes(): List[NodeView] = {
    viewType match {
      case AllFlatByTimeDesc =>
        readApi.allNodesByCreatedDesc
      case AllTreeByTimeDesc =>
        readApi.allNodesAsTreeByCreatedDesc(expandedNodes.toSet)
    }
  }

  def editNode(id: Long): Unit = {
    taskEditorModel.editNode(id)
  }

  def editSelectedNode(): Unit = {
    taskListModel.selectedNode.foreach(selected => taskEditorModel.editNode(selected.id))
  }

  def selectForMove(): Unit = {
    selectedForMove = taskListModel.selectedNode.map(_.id)
  }

  def insertBelow(): Unit = {
    selectedForMove.foreach { idToMove =>
      taskListModel.selectedNode.foreach { newParentNode =>
        writeApi.moveNode(idToMove, Some(ParentInfo(newParentNode.id, 0)))
        refreshTaskList()
      }
    }
  }

  def insertRoot(): Unit = {
    selectedForMove.foreach { idToMove =>
      writeApi.moveNode(idToMove, None)
      refreshTaskList()
    }
  }

  def cycleViewType(): Unit = {
    viewType = nextViewType(viewType)
    refreshTaskList()
  }

  private def nextViewType(current: ViewType): ViewType = {
    val index = AllViewTypes.indexOf(current)
    AllViewTypes(if(index < AllViewTypes.length - 1) index + 1 else 0)
  }

  def flipTodoDone(): Unit = {
    taskListModel.selectedNode.foreach { node =>
      val newStatus = if(node.status == "TODO") {
        "DONE"
      } else {
        "TODO"
      }
      writeApi.changeNodeStatus(node.id, newStatus)
      refreshTaskList()
    }
  }

  def editNewNode(): Unit = {
    taskEditorModel.editNewNode()
  }

  def flipTimestampVisibility(): Unit = {
    taskListModel.flipTimestampVisibility()
  }

  def flipExpandCurrentNode(): Unit = {
    taskListModel.selectedNode.foreach { selectedNode =>
      if(expandedNodes.contains(selectedNode.id)) {
        expandedNodes.remove(selectedNode.id)
      } else {
        expandedNodes += selectedNode.id
      }
      refreshTaskList()
    }
  }

}
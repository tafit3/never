package never.ui

import never.domain.{NodeMatchCondition, NodeView, ParentInfo}
import never.repository.{NodesFilter, RepositoryReadApi, RepositoryWriteApi}

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

  sealed trait SetStatusResult
  case object StatusChangedSuccessfully extends SetStatusResult
  case object NodeNotSelected extends SetStatusResult
  case object StatusWasTheSame extends SetStatusResult
}

class MainFrameModel(taskListModel: TaskListModel,
                     taskEditorModel: TaskEditorModel,
                     readApi: RepositoryReadApi,
                     writeApi: RepositoryWriteApi) {
  import MainFrameModel._
  private var viewType: ViewType = AllTreeByTimeDesc
  private var filteredViewCondition: NodeMatchCondition = NodeMatchCondition.Empty
  private val expandedNodes = mutable.Set.empty[Long]
  private var selectedForMove: Option[Long] = None
  private var textSearchRegex: Option[String] = None

  def applyTextSearchRegex(textSearchRegex: String): Unit = {
    val processedRegex = Option.when(!textSearchRegex.isBlank)(textSearchRegex.trim)
    if(this.textSearchRegex != processedRegex) {
      this.textSearchRegex = processedRegex
      refreshTaskList()
    }
  }


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
    val nodesFilter = NodesFilter(textSearchRegex, filteredViewCondition)
    viewType match {
      case AllFlatByTimeDesc =>
        readApi.allNodesByCreatedDesc(nodesFilter)
      case AllTreeByTimeDesc =>
        readApi.allNodesAsTreeByCreatedDesc(nodesFilter, expandedNodes.toSet)
    }
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

  def switchFilteredView(filteredViewCondition: NodeMatchCondition): Unit = {
    this.filteredViewCondition = filteredViewCondition
    refreshTaskList()
  }

  private def nextViewType(current: ViewType): ViewType = {
    val index = AllViewTypes.indexOf(current)
    AllViewTypes(if(index < AllViewTypes.length - 1) index + 1 else 0)
  }

  def flipTodoDone(): Unit = {
    taskListModel.selectedNode.foreach { node =>
      val newStatus = node.status match {
        case "TODO" => "DONE"
        case "DONE" => "TODO"
        case other => other
      }
      if(newStatus != node.status) {
        writeApi.changeNodeStatus(node.id, newStatus)
        refreshTaskList()
      }
    }
  }

  def setStatusOfSelectedNode(status: String): SetStatusResult = {
    taskListModel.selectedNode match {
      case None =>
        NodeNotSelected
      case Some(node) =>
        if(node.status != status) {
          writeApi.changeNodeStatus(node.id, status)
          refreshTaskList()
          StatusChangedSuccessfully
        } else {
          StatusWasTheSame
        }
    }
  }

  def editNewTaskNode(): Unit = {
    taskEditorModel.editNewNode(None)
  }

  def editNewDataNode(): Unit = {
    taskEditorModel.editNewNode(Some("DATA"))
  }

  def flipTimestampVisibility(): Unit = {
    taskListModel.flipTimestampVisibility()
  }

  def flipExpandCurrentNode(): Unit = {
    if(taskListModel.isFocused()) {
      taskListModel.selectedNode.foreach { selectedNode =>
        if (expandedNodes.contains(selectedNode.id)) {
          expandedNodes.remove(selectedNode.id)
        } else {
          expandedNodes += selectedNode.id
        }
        refreshTaskList()
      }
    }
  }

}
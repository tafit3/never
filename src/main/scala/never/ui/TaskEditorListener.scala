package never.ui

import never.domain.NodeView
import never.ui.TaskEditorModel.EditingState

trait TaskEditorListener {
  def loseFocus(): Unit
  def nodeSaved(id: Long): Unit
  def editingNodeChanged(editingState: EditingState, editingNode: Option[NodeView]): Unit
}

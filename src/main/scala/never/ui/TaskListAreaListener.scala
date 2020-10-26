package never.ui

trait TaskListAreaListener {
  def editNode(id: Long, focusEditor: Boolean): Unit
  def applyFilter(filter: String): Unit
}

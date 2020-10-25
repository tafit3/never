package never.ui

trait TaskListAreaAccessor extends ComponentStateAccessor {
  def requestFocus(): Unit
  def getLineOfOffset(position: Int): Int
  def getCaretPosition: Int
  def getText: String
  def setText(text: String, selectedLine: Int): Unit
}

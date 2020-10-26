package never.ui

trait TaskListAreaAccessor extends ComponentStateAccessor {
  def requestFocus(): Unit
  def getLineOfCaretPosition: Int
  def getText: String
  def setText(text: String, selectedLine: Int): Unit
}

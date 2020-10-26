package never.ui

trait TaskListAreaAccessor extends ComponentStateAccessor {
  def isFocused(): Boolean
  def requestFocus(): Unit
  def getLineOfCaretPosition: Int
  def getText: String
  def setText(text: String, selectedLine: Int): Unit
}

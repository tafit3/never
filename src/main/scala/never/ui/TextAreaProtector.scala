package never.ui

class TextAreaProtector {
  private var editCount = 0

  def editing: Boolean = editCount > 0

  def guard(f: => Unit): Unit = {
    editCount += 1
    try f
    finally editCount -= 1
  }
}

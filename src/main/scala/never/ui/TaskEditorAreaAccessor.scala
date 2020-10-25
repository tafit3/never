package never.ui

trait TaskEditorAreaAccessor extends ComponentStateAccessor {
  def requestFocus(): Unit
  def getText: String
}

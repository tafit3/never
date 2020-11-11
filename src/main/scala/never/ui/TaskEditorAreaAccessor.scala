package never.ui

trait TaskEditorAreaAccessor extends ComponentStateAccessor {
  def requestFocus(): Unit
  def content: String
  def tags: Set[String]
}

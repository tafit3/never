package never.ui

trait StateAccessorSupport[T] {
  private var stateAccessorMaybe: Option[T] = None

  def setStateAccessor(stateAccessor: T): Unit = {
    stateAccessorMaybe = Some(stateAccessor)
  }

  def stateAccessor: T = {
    stateAccessorMaybe.getOrElse {
      throw new IllegalStateException("state accessor not set")
    }
  }

}

package never.ui

trait ListenerSupport[T] {
  protected var listeners = List.empty[T]

  def addListener(listener: T): Unit = {
    listeners ::= listener
  }

  protected def fire(f: T => Unit): Unit = {
    listeners.foreach(f)
  }
}

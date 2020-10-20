package never.util

object CollectionUtils {
  implicit class ListOps[T](list: List[T]) {
    def insert(i: Int, value: T): List[T] = {
      require(i >= 0)
      require(i <= list.size)
      val (front, back) = list.splitAt(i)
      front ++ List(value) ++ back
    }
    def remove(i: Int): List[T] = {
      require(i >= 0)
      require(i < list.size)
      list.patch(i, Nil, 1)
    }
  }
}

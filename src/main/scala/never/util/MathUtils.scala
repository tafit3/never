package never.util

object MathUtils {
  def saturate(x: Int, mi: Int, ma: Int): Int = {
    require(mi <= ma)
    if(x < mi) {
      mi
    } else if(x > ma) {
      ma
    } else {
      x
    }
  }
}

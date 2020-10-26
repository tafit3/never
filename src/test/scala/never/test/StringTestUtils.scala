package never.test

object StringTestUtils {
  implicit class StringOps(s: String) {
    def stripMarginTr: String = {
      s.stripMargin.stripTrailing
    }
  }
}

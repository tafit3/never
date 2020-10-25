package never.util

import java.text.SimpleDateFormat
import java.time.Instant
import java.util.Date

object DateUtils {
  private val dateFormat = new SimpleDateFormat("yyyy-MM-dd")

  def format(instant: Instant): String = {
    dateFormat.format(Date.from(instant))
  }
}

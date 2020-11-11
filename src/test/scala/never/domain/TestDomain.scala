package never.domain

import java.time.Instant
import java.util.concurrent.atomic.AtomicLong

import scala.util.Random

trait TestDomain {
  private val random = new Random()
  private val testSeq = new AtomicLong(0)

  def someInstant = Instant.ofEpochMilli(1_000_000_000_000L)

  def randomStr: String = {
    (1 to 10).map(_ => (random.nextInt(26) + 97).toChar).mkString
  }

  def testId: Long = {
    testSeq.incrementAndGet()
  }

  def someNodeView(created: Instant = Instant.now(),
                   status: String = "TODO",
                   content: String = randomStr,
                   tags: Set[String] = Set.empty,
                   depth: Int = 0,
                   parentInfo: Option[Long] = None,
                   expandable: Boolean = false): NodeView = {
    NodeView(testId, created, status, content, tags, depth, parentInfo, expandable, false)
  }

}

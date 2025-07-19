package analytics.repository

import analytics.model.{Statistics, UserStatistics}
import analytics.repository.InMemoryStatisticsRepository
import org.scalatest.BeforeAndAfter
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.compiletime.uninitialized

class TestInMemoryRepository
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfter:

  private val testStats = UserStatistics(
    userId = "user1",
    testId = "test-123",
    wpm = 10.0,
    accuracy = 95.0,
    errors = List[Int](1, 2, 3),
    timestamp = System.currentTimeMillis()
  )

  before {
    repo = new InMemoryStatisticsRepository()
  }

  after {
    repo.clean()
  }
  private var repo: InMemoryStatisticsRepository = uninitialized

  "InMemoryStatisticsRepository" should "save and retrieve statistics correctly" in {
    val saved = repo.save(testStats)
    repo.get(saved.testId) shouldBe Some(saved)
  }

  it should "return None when getting non-existing statistics" in {
    repo.get("nonexistent") shouldBe None
  }

  it should "list all statistics for a user" in {
    val saved1 = repo.save(testStats)
    val saved2 = repo.save(testStats.copy(testId = "test-456"))

    val userStats = repo.list(testStats.userId)
    userStats should contain allOf (saved1, saved2)
    userStats should have size 2
  }

  it should "return empty list for non-existing user" in {
    repo.list("nonexistent") shouldBe empty
  }

  it should "delete all statistics for a user" in {
    val saved1 = repo.save(testStats)
    val saved2 = repo.save(testStats.copy(testId = "test-789"))

    repo.deleteAll(testStats.userId) shouldBe true
    repo.list(testStats.userId) shouldBe empty
  }

  it should "return false when deleting statistics for non-existing user" in {
    repo.deleteAll("nonexistent") shouldBe false
  }

  it should "handle multiple users independently" in {
    val user1Stats = testStats
    val user2Stats = testStats.copy(userId = "user2", testId = "test-456")

    val saved1 = repo.save(user1Stats)
    val saved2 = repo.save(user2Stats)

    repo.list("user1") should contain only saved1
    repo.list("user2") should contain only saved2
  }

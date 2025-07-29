package analytics.repository

import analytics.model.{Statistics, TestStatistics}
import common.DatabaseInfos
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.{BeforeAndAfter, BeforeAndAfterAll}
import org.testcontainers.containers.MongoDBContainer

import scala.compiletime.uninitialized

class TestMongoStatisticsRepository
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfter
    with BeforeAndAfterAll:

  private val mongoContainer = new MongoDBContainer("mongo:6.0")
  private val testStats = TestStatistics(
    userId = "user1",
    testId = "",
    wpm = 10.0,
    accuracy = 95.0,
    errors = List[Int](1, 2, 3),
    timestamp = System.currentTimeMillis()
  )
  private var repository: MongoStatisticsRepository = uninitialized

  override def beforeAll(): Unit =
    mongoContainer.start()

  override def afterAll(): Unit =
    if repository != null then
      repository.clean()
      repository.close()
    mongoContainer.stop()

  before {
    val dbInfos = DatabaseInfos(
      collectionName = "statistics",
      mongoUri =
        s"mongodb://${mongoContainer.getHost}:${mongoContainer.getFirstMappedPort}",
      databaseName = "statistics_test_db"
    )
    repository = new MongoStatisticsRepository(dbInfos)
    repository.clean()
  }

  after {
    if repository != null then
      repository.clean()
      repository.close()
  }

  "MongoStatisticsRepository" should "save and retrieve statistics correctly" in {
    val saved = repository.save(testStats)
    saved.testId should not be empty
    repository.get(saved.testId) shouldBe Some(saved)
  }

  it should "return None when getting non-existing statistics" in {
    repository.get("507f1f77bcf86cd799439011") shouldBe None
  }

  it should "handle invalid ObjectId format gracefully" in {
    repository.get("invalid-id") shouldBe None
  }

  it should "list all statistics for a user" in {
    val saved1 = repository.save(testStats)
    val saved2 = repository.save(testStats)

    val userStats = repository.list(testStats.userId)
    userStats should contain allOf (saved1, saved2)
    userStats should have size 2
  }

  it should "return empty list for non-existing user" in {
    repository.list("nonexistent") shouldBe empty
  }

  it should "delete all statistics for a user" in {
    val saved1 = repository.save(testStats)
    val saved2 = repository.save(testStats)

    repository.deleteAll(testStats.userId) shouldBe true
    repository.list(testStats.userId) shouldBe empty
  }

  it should "return false when deleting statistics for non-existing user" in {
    repository.deleteAll("nonexistent") shouldBe false
  }

  it should "handle multiple users independently" in {
    val user1Stats = testStats
    val user2Stats = testStats.copy(userId = "user2")

    val saved1 = repository.save(user1Stats)
    val saved2 = repository.save(user2Stats)

    repository.list("user1") should contain only saved1
    repository.list("user2") should contain only saved2
  }

  it should "clean all statistics" in {
    repository.save(testStats)
    repository.save(testStats.copy(userId = "user2"))

    repository.clean() shouldBe true
    repository.list(testStats.userId) shouldBe empty
    repository.list("user2") shouldBe empty
  }

  it should "maintain data consistency across operations" in {
    val saved = repository.save(testStats)
    val retrieved = repository.get(saved.testId).get

    retrieved.userId shouldBe testStats.userId
    retrieved.wpm shouldBe testStats.wpm
    retrieved.accuracy shouldBe testStats.accuracy
    retrieved.errors shouldBe testStats.errors
    retrieved.timestamp shouldBe testStats.timestamp
  }

package typingTest.tests.repository

import com.github.nscala_time.time.Imports.DateTime
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import typingTest.dictionary.model.Dictionary
import typingTest.tests.factory.TypingTestFactory.copy
import typingTest.tests.model.{
  CompletedInfo,
  DefaultContext,
  PersistedTypingTest,
  TypingTest
}

import scala.compiletime.uninitialized

abstract class TypingTestRepositorySpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterEach:

  def createRepository(): TypingTestRepository

  var repository: TypingTestRepository = uninitialized

  override def beforeEach(): Unit =
    repository = createRepository()
    repository.deleteAll()

  def createSampleTest(
      profileId: String = "profile-1",
      language: String = "english"
  ): PersistedTypingTest =
    val dictionary = Dictionary(s"${language}_basic", "/path/to/dict")
    val completedInfo = CompletedInfo(false, None)
    val testData = TypingTest(
      Set(dictionary),
      Seq("lowercase"),
      completedInfo,
      Seq("hello", "world", "test")
    )

    PersistedTypingTest(
      id = None,
      profileId = profileId,
      testData = testData,
      createdAt = DateTime.now(),
      wordCount = 3
    )

  "TypingTestRepository" should {

    "create a new test and assign an ID" in {
      val test = createSampleTest()

      val created = repository.create(test)

      created.id should be(defined)
      created.profileId should equal(test.profileId)
      created.wordCount should equal(test.wordCount)
      created.testData.words should equal(test.testData.words)
    }

    "retrieve a test by ID" in {
      val test = createSampleTest()
      val created = repository.create(test)

      val retrieved = repository.get(created.id.get)

      retrieved should be(defined)
      retrieved.get.id should equal(created.id)
      retrieved.get.profileId should equal(created.profileId)
      retrieved.get.testData.words should equal(created.testData.words)
    }

    "return None when getting non-existent test" in {
      val result = repository.get("non-existent-id")

      result should be(None)
    }

    "delete a test by ID" in {
      val test = createSampleTest()
      val created = repository.create(test)

      val deleted = repository.delete(created.id.get)

      deleted should be(true)
      repository.get(created.id.get) should be(None)
    }

    "return false when deleting non-existent test" in {
      val result = repository.delete("non-existent-id")

      result should be(false)
    }

    "list all tests" in {
      val test1 = createSampleTest("profile-1", "english")
      val test2 = createSampleTest("profile-2", "spanish")

      repository.create(test1)
      repository.create(test2)

      val allTests = repository.list()

      allTests should have size 2
      allTests.map(_.profileId) should contain allElementsOf Seq(
        "profile-1",
        "profile-2"
      )
    }

    "return empty list when no tests exist" in {
      val allTests = repository.list()

      allTests should be(empty)
    }

    "get tests by profile ID" in {
      val test1 = createSampleTest("profile-1", "english")
      val test2 = createSampleTest("profile-1", "spanish")
      val test3 = createSampleTest("profile-2", "english")

      repository.create(test1)
      repository.create(test2)
      repository.create(test3)

      val profile1Tests = repository.getByProfileId("profile-1")

      profile1Tests should have size 2
      profile1Tests.foreach(_.profileId should equal("profile-1"))
    }

    "return empty list for non-existent profile" in {
      val tests = repository.getByProfileId("non-existent-profile")

      tests should be(empty)
    }

    "delete all tests" in {
      val test1 = createSampleTest("profile-1", "english")
      val test2 = createSampleTest("profile-2", "spanish")

      repository.create(test1)
      repository.create(test2)

      val deleted = repository.deleteAll()

      deleted should be(true)
      repository.list() should be(empty)
    }

    "return false when deleting all from empty repository" in {
      val result = repository.deleteAll()

      result should be(false)
    }

    "handle tests with complex data structures" in {
      val dict1 = Dictionary("english_1k", "/path1")
      val dict2 = Dictionary("english_10k", "/path2")
      val completedInfo = CompletedInfo(true, Some(DateTime.now()))
      val testData = TypingTest(
        Set(dict1, dict2),
        Seq("lowercase", "trim", "uppercase"),
        completedInfo,
        Seq("complex", "test", "with", "multiple", "sources")
      )

      val test = PersistedTypingTest(
        id = None,
        profileId = "complex-profile",
        testData = testData,
        createdAt = DateTime.now(),
        wordCount = 5
      )

      val created = repository.create(test)
      val retrieved = repository.get(created.id.get)

      retrieved should be(defined)
      retrieved.get.testData.sources should have size 2
      retrieved.get.testData.modifiers should contain allElementsOf Seq(
        "lowercase",
        "trim",
        "uppercase"
      )
      retrieved.get.testData.words should contain allElementsOf Seq(
        "complex",
        "test",
        "with",
        "multiple",
        "sources"
      )
      retrieved.get.testData.info.completed should be(true)
      retrieved.get.testData.info.completedDateTime should be(defined)
    }

    "preserve DateTime fields correctly" in {
      val now = DateTime.now()
      val completedTime = now.plusMinutes(30)
      val completedInfo = CompletedInfo(true, Some(completedTime))

      val baseTest = createSampleTest()
      val testWithTime = baseTest.copy(
        createdAt = now,
        testData = TypingTest(
          baseTest.testData.sources,
          baseTest.testData.modifiers,
          completedInfo,
          baseTest.testData.words
        )
      )

      val created = repository.create(testWithTime)
      val retrieved = repository.get(created.id.get)

      retrieved should be(defined)
      retrieved.get.createdAt.getMillis should be(now.getMillis +- 1000)
      retrieved.get.testData.info.completedDateTime should be(defined)
    }

    "get last non-completed test by profile ID" in {
      val profileId = "test-profile"

      val test = createSampleTest(profileId, "english")

      Thread.sleep(10)
      val created = repository.create(test)

      val lastTest = repository.getLastNonCompletedByProfileId(profileId)

      lastTest should be(defined)
      lastTest.get.id should equal(created.id)
      lastTest.get.testData.sources.head.name should equal("english_basic")
    }

    "return None when no non-completed tests exist for profile" in {
      val profileId = "test-profile"

      val completedTest = createSampleTest(profileId, "english").copy(
        testData = TypingTest(
          Set(Dictionary("english_basic", "/path/to/dict")),
          Seq("lowercase"),
          CompletedInfo(true, Some(DateTime.now())),
          Seq("completed", "test")
        )
      )
      repository.create(completedTest)

      val lastTest = repository.getLastNonCompletedByProfileId(profileId)

      lastTest should be(None)
    }

    "delete non-completed tests by profile ID" in {
      val profileId = "test-profile"

      val test1 = createSampleTest(profileId, "english")
      val test2 = createSampleTest(profileId, "spanish")
      val completedTest = createSampleTest(profileId, "french").copy(
        testData = TypingTest(
          Set(Dictionary("french_basic", "/path/to/dict")),
          Seq("lowercase"),
          CompletedInfo(true, Some(DateTime.now())),
          Seq("completed", "test")
        )
      )
      val otherProfileTest = createSampleTest("other-profile", "english")

      repository.create(test1)
      repository.create(test2)
      repository.create(completedTest)
      repository.create(otherProfileTest)

      val deletedCount = repository.deleteNonCompletedByProfileId(profileId)

      deletedCount should equal(2)

      val remainingTests = repository.getByProfileId(profileId)
      remainingTests should have size 1
      remainingTests.head.testData.info.completedDateTime should be(defined)

      val otherProfileTests = repository.getByProfileId("other-profile")
      otherProfileTests should have size 1
    }

    "return 0 when deleting non-completed tests for profile with none" in {
      val profileId = "test-profile"

      val completedTest = createSampleTest(profileId, "english").copy(
        testData = TypingTest(
          Set(Dictionary("english_basic", "/path/to/dict")),
          Seq("lowercase"),
          CompletedInfo(true, Some(DateTime.now())),
          Seq("completed", "test")
        )
      )
      repository.create(completedTest)

      val deletedCount = repository.deleteNonCompletedByProfileId(profileId)

      deletedCount should equal(0)
      repository.getByProfileId(profileId) should have size 1
    }

    "get completed test by ID" in {
      val test = createSampleTest("profile-1", "english")
      val created = repository.create(test)

      val completedTest = created.copy(
        testData = created.testData.copy(
          info = CompletedInfo(true, Some(DateTime.now()))
        )
      )
      repository.update(completedTest)

      val retrieved = repository.getCompletedById(created.id.get)

      retrieved should be(defined)
      retrieved.get.id should equal(created.id)
      retrieved.get.testData.info.completedDateTime should be(defined)
    }

    "return None when getting completed test by ID for non-completed test" in {
      val test = createSampleTest("profile-1", "english")
      val created = repository.create(test)

      val retrieved = repository.getCompletedById(created.id.get)

      retrieved should be(None)
    }

    "return None when getting completed test by non-existent ID" in {
      val retrieved = repository.getCompletedById("non-existent-id")

      retrieved should be(None)
    }

    "handle test completion workflow" in {
      val profileId = "test-profile"

      val test1 = createSampleTest(profileId, "english")
      val test2 = createSampleTest(profileId, "spanish")

      val created1 = repository.create(test1)
      val created2 = repository.create(test2)

      repository.deleteNonCompletedByProfileId(profileId)

      val test3 = createSampleTest(profileId, "french")
      val created3 = repository.create(test3)

      val lastTest = repository.getLastNonCompletedByProfileId(profileId)
      lastTest should be(defined)
      lastTest.get.id should equal(created3.id)
      lastTest.get.testData.sources.head.name should equal("french_basic")

      val completedTest = created3.copy(
        testData = created3.testData.copy(
          info = CompletedInfo(true, Some(DateTime.now()))
        )
      )
      repository.update(completedTest)

      val retrievedCompleted = repository.getCompletedById(created3.id.get)
      retrievedCompleted should be(defined)
      retrievedCompleted.get.testData.info.completedDateTime should be(defined)

      val noMoreNonCompleted =
        repository.getLastNonCompletedByProfileId(profileId)
      noMoreNonCompleted should be(None)
    }
  }

package typingTest.tests.repository

import com.github.nscala_time.time.Imports.DateTime
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import typingTest.dictionary.model.Dictionary
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
    val dictionary = Dictionary(s"${language}_basic", language, "/path/to/dict")
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
      language = language,
      wordCount = 3
    )

  "TypingTestRepository" should {

    "create a new test and assign an ID" in {
      val test = createSampleTest()

      val created = repository.create(test)

      created.id should be(defined)
      created.profileId should equal(test.profileId)
      created.language should equal(test.language)
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

    "update an existing test" in {
      val test = createSampleTest()
      val created = repository.create(test)

      val updatedTest = created.copy(language = "spanish")
      val updated = repository.update(updatedTest)

      updated should be(defined)
      updated.get.language should equal("spanish")
      updated.get.id should equal(created.id)
    }

    "return None when updating non-existent test" in {
      val test = createSampleTest().copy(id = Some("non-existent"))

      val result = repository.update(test)

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

    "get tests by language" in {
      val test1 = createSampleTest("profile-1", "english")
      val test2 = createSampleTest("profile-2", "english")
      val test3 = createSampleTest("profile-3", "spanish")

      repository.create(test1)
      repository.create(test2)
      repository.create(test3)

      val englishTests = repository.getByLanguage("english")

      englishTests should have size 2
      englishTests.foreach(_.language should equal("english"))
    }

    "return empty list for non-existent language" in {
      val tests = repository.getByLanguage("non-existent-language")

      tests should be(empty)
    }

    "get tests by profile ID and language" in {
      val test1 = createSampleTest("profile-1", "english")
      val test2 = createSampleTest("profile-1", "spanish")
      val test3 = createSampleTest("profile-2", "english")

      repository.create(test1)
      repository.create(test2)
      repository.create(test3)

      val filteredTests =
        repository.getByProfileIdAndLanguage("profile-1", "english")

      filteredTests should have size 1
      filteredTests.head.profileId should equal("profile-1")
      filteredTests.head.language should equal("english")
    }

    "return empty list for non-matching profile and language combination" in {
      val test1 = createSampleTest("profile-1", "english")
      repository.create(test1)

      val tests = repository.getByProfileIdAndLanguage("profile-1", "spanish")

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
      val dict1 = Dictionary("english_1k", "english", "/path1")
      val dict2 = Dictionary("english_10k", "english", "/path2")
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
        language = "english",
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
      lastTest.get.language should equal("english")
    }

    "return None when no non-completed tests exist for profile" in {
      val profileId = "test-profile"

      val completedTest = createSampleTest(profileId, "english").copy(
        completedAt = Some(DateTime.now()),
        accuracy = Some(95.0),
        rawAccuracy = Some(92.0),
        testTime = Some(30000L),
        errorCount = Some(1),
        errorWordIndices = Some(List(2))
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
        completedAt = Some(DateTime.now()),
        accuracy = Some(88.0)
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
      remainingTests.head.completedAt should be(defined)

      val otherProfileTests = repository.getByProfileId("other-profile")
      otherProfileTests should have size 1
    }

    "return 0 when deleting non-completed tests for profile with none" in {
      val profileId = "test-profile"

      val completedTest = createSampleTest(profileId, "english").copy(
        completedAt = Some(DateTime.now()),
        accuracy = Some(90.0)
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
        completedAt = Some(DateTime.now()),
        accuracy = Some(92.5),
        rawAccuracy = Some(89.0),
        testTime = Some(45000L),
        errorCount = Some(3),
        errorWordIndices = Some(List(1, 4, 7))
      )
      repository.update(completedTest)

      val retrieved = repository.getCompletedById(created.id.get)

      retrieved should be(defined)
      retrieved.get.id should equal(created.id)
      retrieved.get.completedAt should be(defined)
      retrieved.get.accuracy should equal(Some(92.5))
      retrieved.get.rawAccuracy should equal(Some(89.0))
      retrieved.get.testTime should equal(Some(45000L))
      retrieved.get.errorCount should equal(Some(3))
      retrieved.get.errorWordIndices should equal(Some(List(1, 4, 7)))
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
      lastTest.get.language should equal("french")

      val completedTest = created3.copy(
        completedAt = Some(DateTime.now()),
        accuracy = Some(96.0),
        rawAccuracy = Some(94.0),
        testTime = Some(35000L),
        errorCount = Some(1),
        errorWordIndices = Some(List(3))
      )
      repository.update(completedTest)

      val retrievedCompleted = repository.getCompletedById(created3.id.get)
      retrievedCompleted should be(defined)
      retrievedCompleted.get.completedAt should be(defined)

      val noMoreNonCompleted =
        repository.getLastNonCompletedByProfileId(profileId)
      noMoreNonCompleted should be(None)
    }

    "support timeLimit field in tests" in {
      val testWithTimeLimit = createSampleTest("profile-1", "english").copy(
        timeLimit = Some(60000L)
      )

      val created = repository.create(testWithTimeLimit)
      val retrieved = repository.get(created.id.get)

      retrieved should be(defined)
      retrieved.get.timeLimit should equal(Some(60000L))
    }

    "support tests without timeLimit" in {
      val testWithoutTimeLimit = createSampleTest("profile-1", "english")

      val created = repository.create(testWithoutTimeLimit)
      val retrieved = repository.get(created.id.get)

      retrieved should be(defined)
      retrieved.get.timeLimit should be(None)
    }
  }

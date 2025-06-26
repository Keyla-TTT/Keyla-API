package typingTest.tests.repository

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach
import typingTest.tests.model.{CompletedInfo, DefaultContext, PersistedTypingTest, TypingTest}
import typingTest.dictionary.model.Dictionary
import com.github.nscala_time.time.Imports.DateTime
import scala.compiletime.uninitialized

abstract class TypingTestRepositorySpec extends AnyWordSpec with Matchers with BeforeAndAfterEach:

  def createRepository(): TypingTestRepository
  
  var repository: TypingTestRepository = uninitialized

  override def beforeEach(): Unit =
    repository = createRepository()
    repository.deleteAll()

  def createSampleTest(profileId: String = "profile-1", language: String = "english"): PersistedTypingTest =
    val dictionary = Dictionary("sample-dict", language, "/path/to/dict")
    val completedInfo = CompletedInfo(false, None)
    val testData = TypingTest(Set(dictionary), Seq("lowercase"), completedInfo, Seq("hello", "world", "test"))
    
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
      allTests.map(_.profileId) should contain allElementsOf Seq("profile-1", "profile-2")
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
      
      val filteredTests = repository.getByProfileIdAndLanguage("profile-1", "english")
      
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
      val dict1 = Dictionary("dict1", "english", "/path1")
      val dict2 = Dictionary("dict2", "english", "/path2")
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
      retrieved.get.testData.modifiers should contain allElementsOf Seq("lowercase", "trim", "uppercase")
      retrieved.get.testData.words should contain allElementsOf Seq("complex", "test", "with", "multiple", "sources")
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
  }

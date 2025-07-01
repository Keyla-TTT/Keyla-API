package api.services

import org.scalatest.wordspec.AnyWordSpec
import org.scalatest.matchers.should.Matchers
import org.scalatest.BeforeAndAfterEach
import cats.effect.unsafe.implicits.global
import api.models.{CreateProfileRequest, TestRequest, TestResultsRequest}
import api.models.AppError.*
import users_management.repository.InMemoryProfileRepository
import users_management.model.UserProfile
import typingTest.tests.repository.InMemoryTypingTestRepository
import typingTest.dictionary.repository.{
  DictionaryRepository,
  FileDictionaryRepository
}
import java.io.File
import java.nio.file.Files
import scala.compiletime.uninitialized

class TypingTestServiceSpec
    extends AnyWordSpec
    with Matchers
    with BeforeAndAfterEach:

  private var profileRepository: InMemoryProfileRepository = uninitialized
  private var typingTestRepository: InMemoryTypingTestRepository = uninitialized
  private var dictionaryRepository: DictionaryRepository = uninitialized
  private var service: TypingTestService = uninitialized
  private var tempDir: File = uninitialized

  override def beforeEach(): Unit =
    profileRepository = InMemoryProfileRepository()
    typingTestRepository = InMemoryTypingTestRepository()

    tempDir = Files.createTempDirectory("test-dicts").toFile
    createTestDictionaries()
    dictionaryRepository = FileDictionaryRepository(tempDir.getAbsolutePath)

    service = TypingTestService(
      profileRepository,
      dictionaryRepository,
      typingTestRepository
    )

  override def afterEach(): Unit =
    if tempDir != null && tempDir.exists() then
      def deleteRecursively(file: File): Unit =
        if file.isDirectory then
          Option(file.listFiles()).foreach(_.foreach(deleteRecursively))
        file.delete()

      deleteRecursively(tempDir)

  private def createTestDictionaries(): Unit =
    val englishDir = new File(tempDir, "english")
    val spanishDir = new File(tempDir, "spanish")
    englishDir.mkdirs()
    spanishDir.mkdirs()

    val englishDict = new File(englishDir, "english_basic.txt")
    Files.write(
      englishDict.toPath,
      "hello\nworld\ntest\nscala\ntyping\ncode\nprogramming\ndeveloper\nsoftware\ncomputer\nkeyboard\nmouse\nscreen\nfile\ndirectory".getBytes
    )

    val spanishDict = new File(spanishDir, "spanish_basic.txt")
    Files.write(
      spanishDict.toPath,
      "hola\nmundo\nprueba\nescribir\nteclado\ncodigo\nprogramacion\ndesarrollador\nsoftware\nordenador\nraton\npantalla\narchivo\ndirectorio\ncarpeta".getBytes
    )

  private def createTestProfile(): String =
    val profile = UserProfile(
      id = None,
      name = "Test User",
      email = "test@example.com",
      settings = Set.empty
    )
    profileRepository.create(profile).id.get

  "TypingTestService" should {

    "create and persist a typing test successfully" in {
      val profileId = createTestProfile()

      val request = TestRequest(
        profileId = profileId,
        language = "english",
        dictionaryName = "english_basic",
        wordCount = 10,
        modifiers = List("lowercase")
      )

      val result = service.requestTest(request).value.unsafeRunSync()
      result.isRight should be(true)
      val response = result.toOption.get

      response.profileId should equal(profileId)
      response.language should equal("english")
      response.dictionaryName should equal("english_basic")
      response.modifiers should contain("lowercase")
      response.words should have size 10
      response.testId should not be empty
      response.completedAt should be(None)

      val persistedTests = typingTestRepository.getByProfileId(profileId)
      persistedTests should have size 1
      persistedTests.head.id should equal(Some(response.testId))
    }

    "create test with time limit successfully" in {
      val profileId = createTestProfile()

      val request = TestRequest(
        profileId = profileId,
        language = "english",
        dictionaryName = "english_basic",
        wordCount = 5,
        modifiers = List(),
        timeLimit = Some(60000L)
      )

      val result = service.requestTest(request).value.unsafeRunSync()
      result.isRight should be(true)
      val response = result.toOption.get

      response.timeLimit should equal(Some(60000L))

      val persistedTests = typingTestRepository.getByProfileId(profileId)
      persistedTests.head.timeLimit should equal(Some(60000L))
    }

    "delete previous non-completed test when creating new one" in {
      val profileId = createTestProfile()

      val request1 =
        TestRequest(profileId, "english", "english_basic", 5, List())
      val request2 =
        TestRequest(profileId, "english", "english_basic", 3, List())

      service.requestTest(request1).value.unsafeRunSync().isRight should be(
        true
      )
      service.requestTest(request2).value.unsafeRunSync().isRight should be(
        true
      )

      val persistedTests = typingTestRepository.getByProfileId(profileId)
      persistedTests should have size 1
      persistedTests.head.wordCount should equal(3)
    }

    "get last test successfully" in {
      val profileId = createTestProfile()

      val request = TestRequest(
        profileId = profileId,
        language = "english",
        dictionaryName = "english_basic",
        wordCount = 5,
        modifiers = List(),
        timeLimit = Some(30000L)
      )

      service.requestTest(request).value.unsafeRunSync().isRight should be(true)

      val result = service.getLastTest(profileId).value.unsafeRunSync()
      result.isRight should be(true)
      val lastTest = result.toOption.get

      lastTest.words should have size 5
      lastTest.timeLimit should equal(Some(30000L))
    }

    "handle no last test found error" in {
      val profileId = createTestProfile()

      val result = service.getLastTest(profileId).value.unsafeRunSync()

      result.isLeft should be(true)
      result.left.toOption.get shouldBe a[TestNotFound]
    }

    "submit test results successfully" in {
      val profileId = createTestProfile()

      val request =
        TestRequest(profileId, "english", "english_basic", 5, List())
      val testResult = service.requestTest(request).value.unsafeRunSync()
      testResult.isRight should be(true)
      val testId = testResult.toOption.get.testId

      val resultsRequest = TestResultsRequest(
        accuracy = 95.5,
        rawAccuracy = 92.3,
        testTime = 45000L,
        errorCount = 2,
        errorWordIndices = List(1, 3)
      )

      val result =
        service.submitTestResults(testId, resultsRequest).value.unsafeRunSync()
      result.isRight should be(true)
      val response = result.toOption.get

      response.testId should equal(testId)
      response.completedAt should be(defined)
      response.accuracy should equal(Some(95.5))
      response.rawAccuracy should equal(Some(92.3))
      response.testTime should equal(Some(45000L))
      response.errorCount should equal(Some(2))
      response.errorWordIndices should equal(Some(List(1, 3)))
    }

    "handle test already completed error" in {
      val profileId = createTestProfile()

      val request =
        TestRequest(profileId, "english", "english_basic", 5, List())
      val testResult = service.requestTest(request).value.unsafeRunSync()
      val testId = testResult.toOption.get.testId

      val resultsRequest = TestResultsRequest(90.0, 87.0, 40000L, 1, List(2))

      service
        .submitTestResults(testId, resultsRequest)
        .value
        .unsafeRunSync()
        .isRight should be(true)

      val secondResult =
        service.submitTestResults(testId, resultsRequest).value.unsafeRunSync()
      secondResult.isLeft should be(true)
      secondResult.left.toOption.get shouldBe a[TestAlreadyCompleted]
    }

    "retrieve completed test by ID" in {
      val profileId = createTestProfile()

      val request =
        TestRequest(profileId, "english", "english_basic", 5, List())
      val testResult = service.requestTest(request).value.unsafeRunSync()
      val testId = testResult.toOption.get.testId

      val resultsRequest =
        TestResultsRequest(88.0, 85.5, 50000L, 3, List(0, 2, 4))
      service
        .submitTestResults(testId, resultsRequest)
        .value
        .unsafeRunSync()
        .isRight should be(true)

      val retrieveResult = service.getTestById(testId).value.unsafeRunSync()

      retrieveResult.isRight should be(true)
      val persistedTest = retrieveResult.toOption.get

      persistedTest.testId should equal(testId)
      persistedTest.profileId should equal(profileId)
      persistedTest.completedAt should be(defined)
      persistedTest.accuracy should equal(Some(88.0))
      persistedTest.rawAccuracy should equal(Some(85.5))
      persistedTest.testTime should equal(Some(50000L))
      persistedTest.errorCount should equal(Some(3))
      persistedTest.errorWordIndices should equal(Some(List(0, 2, 4)))
    }

    "return test not found for non-completed test" in {
      val profileId = createTestProfile()

      val request =
        TestRequest(profileId, "english", "english_basic", 5, List())
      val testResult = service.requestTest(request).value.unsafeRunSync()
      val testId = testResult.toOption.get.testId

      val retrieveResult = service.getTestById(testId).value.unsafeRunSync()

      retrieveResult.isLeft should be(true)
      retrieveResult.left.toOption.get shouldBe a[TestNotFound]
    }

    "handle profile not found error" in {
      val request = TestRequest(
        profileId = "non-existent-profile",
        language = "english",
        dictionaryName = "english_basic",
        wordCount = 10,
        modifiers = List()
      )

      val result = service.requestTest(request).value.unsafeRunSync()

      result.isLeft should be(true)
      result.left.toOption.get shouldBe a[ProfileNotFound]
    }

    "handle dictionary not found error" in {
      val profileId = createTestProfile()

      val request = TestRequest(
        profileId = profileId,
        language = "english",
        dictionaryName = "non_existent",
        wordCount = 10,
        modifiers = List()
      )

      val result = service.requestTest(request).value.unsafeRunSync()

      result.isLeft should be(true)
      result.left.toOption.get shouldBe a[DictionaryNotFound]
    }

    "handle language not supported error" in {
      val profileId = createTestProfile()

      val request = TestRequest(
        profileId = profileId,
        language = "unsupported-language",
        dictionaryName = "any_dict",
        wordCount = 10,
        modifiers = List()
      )

      val result = service.requestTest(request).value.unsafeRunSync()

      result.isLeft should be(true)
      result.left.toOption.get shouldBe a[DictionaryNotFound]
    }

    "handle invalid modifier error" in {
      val profileId = createTestProfile()

      val request = TestRequest(
        profileId = profileId,
        language = "english",
        dictionaryName = "english_basic",
        wordCount = 10,
        modifiers = List("invalid-modifier")
      )

      val result = service.requestTest(request).value.unsafeRunSync()

      result.isLeft should be(true)
      result.left.toOption.get shouldBe a[InvalidModifier]
    }

    "create profile successfully" in {
      val request = CreateProfileRequest(
        name = "New User",
        email = "newuser@example.com",
        settings = Set("setting1", "setting2")
      )

      val result = service.createProfile(request).value.unsafeRunSync()

      result.isRight should be(true)
      val profile = result.toOption.get

      profile.name should equal("New User")
      profile.email should equal("newuser@example.com")
      profile.settings should equal(Set("setting1", "setting2"))
      profile.id should not be empty
    }

    "list all profiles successfully" in {
      val request1 = CreateProfileRequest(
        name = "User One",
        email = "user1@example.com",
        settings = Set("setting1")
      )
      val request2 = CreateProfileRequest(
        name = "User Two",
        email = "user2@example.com",
        settings = Set("setting2")
      )

      service.createProfile(request1).value.unsafeRunSync().isRight should be(
        true
      )
      service.createProfile(request2).value.unsafeRunSync().isRight should be(
        true
      )

      val result = service.getAllProfiles().value.unsafeRunSync()

      result.isRight should be(true)
      val profileList = result.toOption.get

      profileList.profiles should have size 2
      profileList.profiles.map(_.name) should contain allElementsOf List(
        "User One",
        "User Two"
      )
    }
  }

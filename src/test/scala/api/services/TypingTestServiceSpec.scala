package api.services

import api.models.typingtest.*
import api.models.users.*
import api.models.AppError.*
import api.models.AppResult
import cats.effect.IO
import cats.effect.unsafe.implicits.global
import org.scalatest.BeforeAndAfterEach
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import typingTest.dictionary.loader.FileDictionaryLoader
import typingTest.dictionary.model.{Dictionary, DictionaryJson}
import typingTest.dictionary.repository.FileDictionaryRepository
import typingTest.tests.repository.InMemoryTypingTestRepository
import users_management.model.UserProfile
import users_management.repository.InMemoryProfileRepository
import analytics.repository.InMemoryStatisticsRepository
import com.github.plokhotnyuk.jsoniter_scala.core.*

import java.io.File
import java.nio.file.Files
import scala.compiletime.uninitialized

class TypingTestServiceSpec
    extends AnyFlatSpec
    with Matchers
    with BeforeAndAfterEach:

  var service: TypingTestService = uninitialized
  var profileRepository: InMemoryProfileRepository = uninitialized
  var dictionaryRepository: FileDictionaryRepository = uninitialized
  var typingTestRepository: InMemoryTypingTestRepository = uninitialized
  var statisticsRepository: InMemoryStatisticsRepository = uninitialized
  var tempDir: File = uninitialized

  override def beforeEach(): Unit =
    profileRepository = InMemoryProfileRepository()
    typingTestRepository = InMemoryTypingTestRepository()
    statisticsRepository = InMemoryStatisticsRepository()

    tempDir = Files.createTempDirectory("test-dicts").toFile
    createTestDictionaries()
    dictionaryRepository = FileDictionaryRepository(tempDir.getAbsolutePath)

    service = TypingTestService(
      profileRepository,
      dictionaryRepository,
      typingTestRepository,
      statisticsRepository
    )

  override def afterEach(): Unit =
    if tempDir != null && tempDir.exists() then
      def deleteRecursively(file: File): Unit =
        if file.isDirectory then
          Option(file.listFiles()).foreach(_.foreach(deleteRecursively))
        file.delete()

      deleteRecursively(tempDir)

  private def createTestDictionaries(): Unit =
    val englishDict = DictionaryJson(
      "english_basic",
      Seq(
        "hello",
        "world",
        "test",
        "scala",
        "typing",
        "code",
        "programming",
        "developer",
        "software",
        "computer",
        "keyboard",
        "mouse",
        "screen",
        "file",
        "directory"
      )
    )
    val spanishDict = DictionaryJson(
      "spanish_basic",
      Seq(
        "hola",
        "mundo",
        "prueba",
        "escribir",
        "teclado",
        "codigo",
        "programacion",
        "desarrollador",
        "software",
        "ordenador",
        "raton",
        "pantalla",
        "archivo",
        "directorio",
        "carpeta"
      )
    )

    Files.write(
      new File(tempDir, "english_basic.json").toPath,
      writeToString(englishDict).getBytes
    )

    Files.write(
      new File(tempDir, "spanish_basic.json").toPath,
      writeToString(spanishDict).getBytes
    )

  private def createTestProfile(): String =
    val profile = UserProfile(
      id = None,
      name = "Test User",
      email = "test@example.com",
      settings = Set.empty
    )
    profileRepository.create(profile).id.get

  "TypingTestService" should "create and persist a typing test successfully" in {
    val profileId = createTestProfile()

    val request = TestRequest(
      profileId = profileId,
      sources = List(SourceWithMerger("english_basic")),
      wordCount = 10,
      modifiers = List("lowercase")
    )

    val result = service.requestTest(request).value.unsafeRunSync()
    result.isRight should be(true)
    val response = result.toOption.get

    response.profileId should equal(profileId)
    response.sources should have size 1
    response.sources.head.name should equal("english_basic")
    response.words.size should equal(10)
    response.modifiers should contain("lowercase")
    response.testId should not be empty
    response.completedAt should be(None)

    val persistedTests = typingTestRepository.getByProfileId(profileId)
    persistedTests should have size 1
    persistedTests.head.id should equal(Some(response.testId))
  }

  it should "create test with time limit successfully" in {
    val profileId = createTestProfile()

    val request = TestRequest(
      profileId = profileId,
      sources = List(SourceWithMerger("english_basic")),
      wordCount = 5,
      modifiers = List(),
      timeLimit = Some(60000L)
    )

    val result = service.requestTest(request).value.unsafeRunSync()
    result.isRight should be(true)
    val response = result.toOption.get

    response.testId should not be empty
    response.completedAt should be(None)

    val persistedTests = typingTestRepository.getByProfileId(profileId)
    persistedTests should have size 1
    persistedTests.head.id should equal(Some(response.testId))
  }

  it should "delete previous non-completed test when creating new one" in {
    val profileId = createTestProfile()

    val request1 =
      TestRequest(profileId, List(SourceWithMerger("english_basic")), 5, List())
    val request2 =
      TestRequest(profileId, List(SourceWithMerger("english_basic")), 3, List())

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

  it should "get last test successfully" in {
    val profileId = createTestProfile()

    val request = TestRequest(
      profileId = profileId,
      sources = List(SourceWithMerger("english_basic")),
      wordCount = 5,
      modifiers = List(),
      timeLimit = Some(30000L)
    )

    service.requestTest(request).value.unsafeRunSync().isRight should be(true)

    val result = service.getLastTest(profileId).value.unsafeRunSync()
    result.isRight should be(true)
    val lastTest = result.toOption.get

    lastTest.words should have size 5
  }

  it should "handle no last test found error" in {
    val result =
      service.getLastTest("non-existent-profile").value.unsafeRunSync()
    result.isLeft should be(true)
    result.left.toOption.get shouldBe a[ProfileNotFound]
  }

  it should "submit test results successfully" in {
    val profileId = createTestProfile()

    val request = TestRequest(
      profileId = profileId,
      sources = List(SourceWithMerger("english_basic")),
      wordCount = 5,
      modifiers = List()
    )

    val testResult = service.requestTest(request).value.unsafeRunSync()
    testResult.isRight should be(true)
    val testId = testResult.toOption.get.testId

    val resultsRequest = TestResultsRequest(
      accuracy = 95.5,
      rawAccuracy = 94.2,
      testTime = 30000L,
      errorCount = 2,
      errorWordIndices = List(1, 3)
    )

    val result =
      service.submitTestResults(testId, resultsRequest).value.unsafeRunSync()
    result.isRight should be(true)
    val response = result.toOption.get

    response.testId should equal(testId)
    response.completedAt should be(defined)
  }

  it should "handle test already completed error" in {
    val profileId = createTestProfile()

    val request = TestRequest(
      profileId = profileId,
      sources = List(SourceWithMerger("english_basic")),
      wordCount = 5,
      modifiers = List()
    )

    val testResult = service.requestTest(request).value.unsafeRunSync()
    testResult.isRight should be(true)
    val testId = testResult.toOption.get.testId

    val resultsRequest = TestResultsRequest(
      accuracy = 95.5,
      rawAccuracy = 94.2,
      testTime = 30000L,
      errorCount = 2,
      errorWordIndices = List(1, 3)
    )

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

  it should "retrieve completed test by ID" in {
    val profileId = createTestProfile()

    val request = TestRequest(
      profileId = profileId,
      sources = List(SourceWithMerger("english_basic")),
      wordCount = 5,
      modifiers = List()
    )

    val testResult = service.requestTest(request).value.unsafeRunSync()
    testResult.isRight should be(true)
    val testId = testResult.toOption.get.testId

    val resultsRequest = TestResultsRequest(
      accuracy = 95.5,
      rawAccuracy = 94.2,
      testTime = 30000L,
      errorCount = 2,
      errorWordIndices = List(1, 3)
    )

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
  }

  it should "return test not found for non-completed test" in {
    val profileId = createTestProfile()

    val request = TestRequest(
      profileId = profileId,
      sources = List(SourceWithMerger("english_basic")),
      wordCount = 5,
      modifiers = List()
    )

    val testResult = service.requestTest(request).value.unsafeRunSync()
    testResult.isRight should be(true)
    val testId = testResult.toOption.get.testId

    val retrieveResult = service.getTestById(testId).value.unsafeRunSync()
    retrieveResult.isLeft should be(true)
    retrieveResult.left.toOption.get shouldBe a[TestNotFound]
  }

  it should "handle profile not found error" in {
    val request = TestRequest(
      profileId = "non-existent-profile",
      sources = List(SourceWithMerger("english_basic")),
      wordCount = 10,
      modifiers = List("lowercase")
    )

    val result = service.requestTest(request).value.unsafeRunSync()
    result.isLeft should be(true)
    result.left.toOption.get shouldBe a[ProfileNotFound]
  }

  it should "handle dictionary not found error" in {
    val profileId = createTestProfile()

    val request = TestRequest(
      profileId = profileId,
      sources = List(SourceWithMerger("non-existent-dictionary")),
      wordCount = 10,
      modifiers = List("lowercase")
    )

    val result = service.requestTest(request).value.unsafeRunSync()
    result.isLeft should be(true)
    result.left.toOption.get shouldBe a[DictionaryNotFound]
  }

  it should "handle language not supported error" in {
    val profileId = createTestProfile()

    val request = TestRequest(
      profileId = profileId,
      sources = List(SourceWithMerger("unsupported-language")),
      wordCount = 10,
      modifiers = List("lowercase")
    )

    val result = service.requestTest(request).value.unsafeRunSync()
    result.isLeft should be(true)
    result.left.toOption.get shouldBe a[DictionaryNotFound]
  }

  it should "handle invalid modifier error" in {
    val profileId = createTestProfile()

    val request = TestRequest(
      profileId = profileId,
      sources = List(SourceWithMerger("english_basic")),
      wordCount = 10,
      modifiers = List("invalid-modifier")
    )

    val result = service.requestTest(request).value.unsafeRunSync()
    result.isLeft should be(true)
    result.left.toOption.get shouldBe a[InvalidModifier]
  }

  it should "create a typing test with two sources and concatenate merger" in {
    val profileId = createTestProfile()
    val request = TestRequest(
      profileId = profileId,
      sources = List(
        SourceWithMerger("english_basic"),
        SourceWithMerger("spanish_basic", Some("concatenate"))
      ),
      wordCount = 30,
      modifiers = List("lowercase")
    )
    val result = service.requestTest(request).value.unsafeRunSync()
    result.isRight should be(true)
    val response = result.toOption.get
    response.sources should have size 2
    response.sources.map(_.name) should contain allElementsOf List(
      "english_basic",
      "spanish_basic"
    )
    response.words.size should be <= 30
    val englishWords = Set(
      "hello",
      "world",
      "test",
      "scala",
      "typing",
      "code",
      "programming",
      "developer",
      "software",
      "computer",
      "keyboard",
      "mouse",
      "screen",
      "file",
      "directory"
    )
    val spanishWords = Set(
      "hola",
      "mundo",
      "prueba",
      "escribir",
      "teclado",
      "codigo",
      "programacion",
      "desarrollador",
      "software",
      "ordenador",
      "raton",
      "pantalla",
      "archivo",
      "directorio",
      "carpeta"
    )
    val allWords = englishWords ++ spanishWords
    response.words.forall(w => allWords.contains(w)) should be(true)
  }

  it should "fail if a merger is missing for a second source" in {
    val profileId = createTestProfile()
    val request = TestRequest(
      profileId = profileId,
      sources = List(
        SourceWithMerger("english_basic"),
        SourceWithMerger("spanish_basic") // No merger specified
      ),
      wordCount = 10
    )
    val result = service.requestTest(request).value.unsafeRunSync()
    result.isLeft should be(true)
    result.left.toOption.get shouldBe a[TestCreationFailed]
  }

  it should "fail if an invalid merger name is provided" in {
    val profileId = createTestProfile()
    val request = TestRequest(
      profileId = profileId,
      sources = List(
        SourceWithMerger("english_basic"),
        SourceWithMerger("spanish_basic", Some("notamerger"))
      ),
      wordCount = 10
    )
    val result = service.requestTest(request).value.unsafeRunSync()
    result.isLeft should be(true)
    result.left.toOption.get shouldBe a[ValidationError]
  }

  it should "merge words using alternate merger" in {
    val profileId = createTestProfile()
    val request = TestRequest(
      profileId = profileId,
      sources = List(
        SourceWithMerger("english_basic"),
        SourceWithMerger("spanish_basic", Some("alternate"))
      ),
      wordCount = 10
    )
    val result = service.requestTest(request).value.unsafeRunSync()
    result.isRight should be(true)
    val response = result.toOption.get
    response.words.size should be <= 10
    // Should alternate between english and spanish words
    val englishWords = Set(
      "hello",
      "world",
      "test",
      "scala",
      "typing",
      "code",
      "programming",
      "developer",
      "software",
      "computer",
      "keyboard",
      "mouse",
      "screen",
      "file",
      "directory"
    )
    val spanishWords = Set(
      "hola",
      "mundo",
      "prueba",
      "escribir",
      "teclado",
      "codigo",
      "programacion",
      "desarrollador",
      "software",
      "ordenador",
      "raton",
      "pantalla",
      "archivo",
      "directorio",
      "carpeta"
    )
    val alternated =
      response.words.take(10).zipWithIndex.forall { case (w, i) =>
        if i % 2 == 0 then englishWords.contains(w)
        else spanishWords.contains(w)
      }
    alternated should be(true)
  }

  it should "get all available modifiers" in {
    val result = service.getAllModifiers().value.unsafeRunSync()
    result.isRight should be(true)

    val response = result.toOption.get
    response.modifiers should not be empty
    response.modifiers.foreach { modifier =>
      modifier.name should not be empty
      modifier.description should not be empty
    }
  }

  it should "get all available mergers" in {
    val result = service.getAllMergers().value.unsafeRunSync()
    result.isRight should be(true)

    val response = result.toOption.get
    response.mergers should not be empty
    response.mergers.foreach { merger =>
      merger.name should not be empty
      merger.description should not be empty
    }
  }

  private def writeToString(dict: DictionaryJson): String =
    com.github.plokhotnyuk.jsoniter_scala.core.writeToString(dict)

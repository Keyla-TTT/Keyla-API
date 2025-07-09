package api.integration

import api.controllers.{ConfigurationController, TypingTestController}
import api.models.*
import api.models.ApiModels.given
import api.server.ApiServer
import api.services.TypingTestService
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.github.plokhotnyuk.jsoniter_scala.core.*
import config.*
import org.http4s.blaze.client.BlazeClientBuilder
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import sttp.client3.*
import sttp.client3.http4s.Http4sBackend
import sttp.client3.jsoniter.*
import sttp.model.{StatusCode, Uri}
import sttp.tapir.json.jsoniter.*
import typingTest.dictionary.repository.{
  DictionaryRepository,
  FileDictionaryRepository
}
import typingTest.tests.repository.InMemoryTypingTestRepository
import users_management.repository.InMemoryProfileRepository

import java.io.File
import java.nio.file.Files
import scala.compiletime.uninitialized
import scala.concurrent.duration.*
import scala.util.Random

class ApiIntegrationSpec
    extends AsyncWordSpec
    with AsyncIOSpec
    with Matchers
    with BeforeAndAfterEach:

  private var profileRepository: InMemoryProfileRepository = uninitialized
  private var typingTestRepository: InMemoryTypingTestRepository = uninitialized
  private var dictionaryRepository: DictionaryRepository = uninitialized
  private var tempDir: File = uninitialized
  private var testPort: Int = uninitialized
  private var baseUri: Uri = uninitialized

  override def beforeEach(): Unit =
    profileRepository = InMemoryProfileRepository()
    typingTestRepository = InMemoryTypingTestRepository()

    tempDir = Files.createTempDirectory("test-dicts").toFile
    createTestDictionaries()
    dictionaryRepository = FileDictionaryRepository(tempDir.getAbsolutePath)

    testPort = 8080 + Random.nextInt(1000)
    baseUri = uri"http://localhost:$testPort"

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

  private def withServer[T](test: SttpBackend[IO, Any] => IO[T]): IO[T] =
    val service = TypingTestService(
      profileRepository,
      dictionaryRepository,
      typingTestRepository
    )

    // Create default configuration for testing
    val testConfig = AppConfig(
      database = DatabaseConfig(useMongoDb = false),
      server = ServerConfig(host = "localhost", port = testPort),
      dictionary =
        DictionaryConfig(basePath = "src/test/resources/dictionaries")
    )

    // Create configuration service and controller
    val configServiceIO = ConfigurationService.create(
      testConfig,
      profileRepository,
      typingTestRepository,
      dictionaryRepository
    )

    configServiceIO.flatMap { configService =>
      val configController = ConfigurationController(configService)
      val controller = TypingTestController(service, configController)
      val server = ApiServer(controller)

      server.resource(testPort, "localhost").use { _ =>
        BlazeClientBuilder[IO].resource.use { client =>
          val backend = Http4sBackend.usingClient(client)
          IO.sleep(200.millis) *> test(backend)
        }
      }
    }

  "API Integration Tests" should {

    "create profile successfully" in {
      withServer { backend =>
        val request = CreateProfileRequest(
          name = "Integration Test User",
          email = "integration@test.com",
          settings = Set("setting1", "setting2")
        )

        val response = basicRequest
          .post(baseUri.addPath("api", "profiles"))
          .body(request)
          .response(asJson[ProfileResponse])
          .send(backend)

        response.map { result =>
          result.code shouldBe StatusCode.Created
          result.body.isRight shouldBe true

          val profile = result.body.toOption.get
          profile.name shouldBe "Integration Test User"
          profile.email shouldBe "integration@test.com"
          profile.settings shouldBe Set("setting1", "setting2")
          profile.id should not be empty
        }
      }
    }

    "get all profiles successfully" in {
      withServer { backend =>
        val createRequest = CreateProfileRequest(
          name = "Test User 1",
          email = "user1@test.com",
          settings = Set.empty
        )

        for
          _ <- basicRequest
            .post(baseUri.addPath("api", "profiles"))
            .body(createRequest)
            .response(asJson[ProfileResponse])
            .send(backend)
          _ <- basicRequest
            .post(baseUri.addPath("api", "profiles"))
            .body(
              createRequest.copy(name = "Test User 2", email = "user2@test.com")
            )
            .response(asJson[ProfileResponse])
            .send(backend)

          listResponse <- basicRequest
            .get(baseUri.addPath("api", "profiles"))
            .response(asJson[ProfileListResponse])
            .send(backend)
        yield
          listResponse.code shouldBe StatusCode.Ok
          listResponse.body.isRight shouldBe true

          val profileList = listResponse.body.toOption.get
          profileList.profiles should have size 2
      }
    }

    "create test successfully" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )

        for
          profileResponse <- basicRequest
            .post(baseUri.addPath("api", "profiles"))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profileId = profile.id,
            language = "english",
            dictionaryName = "english_basic",
            wordCount = 10,
            modifiers = List("lowercase")
          )

          testResponse <- basicRequest
            .post(baseUri.addPath("api", "tests"))
            .body(testRequest)
            .response(asJson[TestResponse])
            .send(backend)
        yield
          testResponse.code shouldBe StatusCode.Created
          testResponse.body.isRight shouldBe true

          val test = testResponse.body.toOption.get
          test.profileId shouldBe profile.id
          test.language shouldBe "english"
          test.modifiers should contain("lowercase")
          test.words should have size 10
          test.testId should not be empty
          test.completedAt shouldBe None
      }
    }

    "create test with time limit successfully" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )

        for
          profileResponse <- basicRequest
            .post(baseUri.addPath("api", "profiles"))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profileId = profile.id,
            language = "english",
            dictionaryName = "english_basic",
            wordCount = 10,
            modifiers = List("lowercase"),
            timeLimit = Some(60000L)
          )

          testResponse <- basicRequest
            .post(baseUri.addPath("api", "tests"))
            .body(testRequest)
            .response(asJson[TestResponse])
            .send(backend)
        yield
          testResponse.code shouldBe StatusCode.Created
          testResponse.body.isRight shouldBe true

          val test = testResponse.body.toOption.get
          test.timeLimit shouldBe Some(60000L)
      }
    }

    "get last test successfully" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )

        for
          profileResponse <- basicRequest
            .post(baseUri.addPath("api", "profiles"))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profileId = profile.id,
            language = "english",
            dictionaryName = "english_basic",
            wordCount = 5,
            modifiers = List(),
            timeLimit = Some(30000L)
          )

          _ <- basicRequest
            .post(baseUri.addPath("api", "tests"))
            .body(testRequest)
            .response(asJson[TestResponse])
            .send(backend)

          lastTestResponse <- basicRequest
            .get(baseUri.addPath("api", "profiles", profile.id, "last-test"))
            .response(asJson[LastTestResponse])
            .send(backend)
        yield
          lastTestResponse.code shouldBe StatusCode.Ok
          lastTestResponse.body.isRight shouldBe true

          val lastTest = lastTestResponse.body.toOption.get
          lastTest.words should have size 5
          lastTest.timeLimit shouldBe Some(30000L)
      }
    }

    "submit test results successfully" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )

        for
          profileResponse <- basicRequest
            .post(baseUri.addPath("api", "profiles"))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profileId = profile.id,
            language = "english",
            dictionaryName = "english_basic",
            wordCount = 5,
            modifiers = List()
          )

          testResponse <- basicRequest
            .post(baseUri.addPath("api", "tests"))
            .body(testRequest)
            .response(asJson[TestResponse])
            .send(backend)

          test = testResponse.body.toOption.get

          resultsRequest = TestResultsRequest(
            accuracy = 95.5,
            rawAccuracy = 92.3,
            testTime = 45000L,
            errorCount = 2,
            errorWordIndices = List(1, 3)
          )

          resultsResponse <- basicRequest
            .put(baseUri.addPath("api", "tests", test.testId, "results"))
            .body(resultsRequest)
            .response(asJson[TestResponse])
            .send(backend)
        yield
          resultsResponse.code shouldBe StatusCode.Ok
          resultsResponse.body.isRight shouldBe true

          val completedTest = resultsResponse.body.toOption.get
          completedTest.testId shouldBe test.testId
          completedTest.completedAt shouldBe defined
          completedTest.accuracy shouldBe Some(95.5)
          completedTest.rawAccuracy shouldBe Some(92.3)
          completedTest.testTime shouldBe Some(45000L)
          completedTest.errorCount shouldBe Some(2)
          completedTest.errorWordIndices shouldBe Some(List(1, 3))
      }
    }

    "get completed test by ID successfully" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )

        for
          profileResponse <- basicRequest
            .post(baseUri.addPath("api", "profiles"))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profileId = profile.id,
            language = "english",
            dictionaryName = "english_basic",
            wordCount = 5,
            modifiers = List()
          )

          testResponse <- basicRequest
            .post(baseUri.addPath("api", "tests"))
            .body(testRequest)
            .response(asJson[TestResponse])
            .send(backend)

          test = testResponse.body.toOption.get

          resultsRequest = TestResultsRequest(
            accuracy = 88.0,
            rawAccuracy = 85.5,
            testTime = 50000L,
            errorCount = 3,
            errorWordIndices = List(0, 2, 4)
          )

          _ <- basicRequest
            .put(baseUri.addPath("api", "tests", test.testId, "results"))
            .body(resultsRequest)
            .response(asJson[TestResponse])
            .send(backend)

          getResponse <- basicRequest
            .get(baseUri.addPath("api", "tests", test.testId))
            .response(asJson[TestResponse])
            .send(backend)
        yield
          getResponse.code shouldBe StatusCode.Ok
          getResponse.body.isRight shouldBe true

          val persistedTest = getResponse.body.toOption.get
          persistedTest.testId shouldBe test.testId
          persistedTest.profileId shouldBe profile.id
          persistedTest.completedAt shouldBe defined
          persistedTest.accuracy shouldBe Some(88.0)
          persistedTest.rawAccuracy shouldBe Some(85.5)
          persistedTest.testTime shouldBe Some(50000L)
          persistedTest.errorCount shouldBe Some(3)
          persistedTest.errorWordIndices shouldBe Some(List(0, 2, 4))
      }
    }

    "get tests by profile ID successfully" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )

        for
          profileResponse <- basicRequest
            .post(baseUri.addPath("api", "profiles"))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest1 = TestRequest(
            profile.id,
            "english",
            "english_basic",
            5,
            List("lowercase")
          )
          testRequest2 = TestRequest(
            profile.id,
            "spanish",
            "spanish_basic",
            3,
            List("uppercase")
          )

          testResponse1 <- basicRequest
            .post(baseUri.addPath("api", "tests"))
            .body(testRequest1)
            .response(asJson[TestResponse])
            .send(backend)

          resultsRequest = TestResultsRequest(85.0, 82.0, 40000L, 1, List(2))

          _ <- basicRequest
            .put(
              baseUri.addPath(
                "api",
                "tests",
                testResponse1.body.toOption.get.testId,
                "results"
              )
            )
            .body(resultsRequest)
            .response(asJson[TestResponse])
            .send(backend)

          testResponse2 <- basicRequest
            .post(baseUri.addPath("api", "tests"))
            .body(testRequest2)
            .response(asJson[TestResponse])
            .send(backend)

          _ <- basicRequest
            .put(
              baseUri.addPath(
                "api",
                "tests",
                testResponse2.body.toOption.get.testId,
                "results"
              )
            )
            .body(resultsRequest)
            .response(asJson[TestResponse])
            .send(backend)

          testsResponse <- basicRequest
            .get(baseUri.addPath("api", "profiles", profile.id, "tests"))
            .response(asJson[TestListResponse])
            .send(backend)
        yield
          testsResponse.code shouldBe StatusCode.Ok
          testsResponse.body.isRight shouldBe true

          val testList = testsResponse.body.toOption.get
          testList.tests should have size 2
          testList.tests.foreach(_.profileId shouldBe profile.id)
          testList.tests.map(_.language) should contain allElementsOf List(
            "english",
            "spanish"
          )
          testList.tests.foreach(_.completedAt shouldBe defined)
      }
    }

    "get tests by language successfully" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )

        for
          profileResponse <- basicRequest
            .post(baseUri.addPath("api", "profiles"))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profile.id,
            "english",
            "english_basic",
            5,
            List()
          )

          testResponse <- basicRequest
            .post(baseUri.addPath("api", "tests"))
            .body(testRequest)
            .response(asJson[TestResponse])
            .send(backend)

          test = testResponse.body.toOption.get

          resultsRequest = TestResultsRequest(90.0, 87.5, 35000L, 1, List(3))

          _ <- basicRequest
            .put(baseUri.addPath("api", "tests", test.testId, "results"))
            .body(resultsRequest)
            .response(asJson[TestResponse])
            .send(backend)

          testsResponse <- basicRequest
            .get(baseUri.addPath("api", "tests", "language", "english"))
            .response(asJson[TestListResponse])
            .send(backend)
        yield
          testsResponse.code shouldBe StatusCode.Ok
          testsResponse.body.isRight shouldBe true

          val testList = testsResponse.body.toOption.get
          testList.tests should have size 1
          testList.tests.head.language shouldBe "english"
          testList.tests.head.completedAt shouldBe defined
      }
    }

    "delete previous non-completed test when creating new one" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )

        for
          profileResponse <- basicRequest
            .post(baseUri.addPath("api", "profiles"))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest1 = TestRequest(
            profile.id,
            "english",
            "english_basic",
            5,
            List()
          )
          testRequest2 = TestRequest(
            profile.id,
            "english",
            "english_basic",
            3,
            List()
          )

          _ <- basicRequest
            .post(baseUri.addPath("api", "tests"))
            .body(testRequest1)
            .response(asJson[TestResponse])
            .send(backend)

          _ <- basicRequest
            .post(baseUri.addPath("api", "tests"))
            .body(testRequest2)
            .response(asJson[TestResponse])
            .send(backend)

          lastTestResponse <- basicRequest
            .get(baseUri.addPath("api", "profiles", profile.id, "last-test"))
            .response(asJson[LastTestResponse])
            .send(backend)
        yield
          lastTestResponse.code shouldBe StatusCode.Ok
          lastTestResponse.body.isRight shouldBe true

          val lastTest = lastTestResponse.body.toOption.get
          lastTest.words should have size 3
      }
    }

    "handle profile not found error" in {
      withServer { backend =>
        val testRequest = TestRequest(
          profileId = "non-existent-profile",
          language = "english",
          dictionaryName = "english_basic",
          wordCount = 10,
          modifiers = List()
        )

        val response = basicRequest
          .post(baseUri.addPath("api", "tests"))
          .body(testRequest)
          .response(asString)
          .send(backend)

        response.map { result =>
          result.code shouldBe StatusCode.NotFound

          val errorBody = result.body match
            case Left(errorString) => errorString
            case Right(bodyString) => bodyString
          val error = readFromString[ErrorResponse](errorBody)
          error.code shouldBe "PROFILE_NOT_FOUND"
          error.message should include("non-existent-profile")
        }
      }
    }

    "handle test not found error for non-completed test" in {
      withServer { backend =>
        val response = basicRequest
          .get(baseUri.addPath("api", "tests", "non-existent-test"))
          .response(asString)
          .send(backend)

        response.map { result =>
          result.code shouldBe StatusCode.NotFound

          val errorBody = result.body match
            case Left(errorString) => errorString
            case Right(bodyString) => bodyString
          val error = readFromString[ErrorResponse](errorBody)
          error.code shouldBe "TEST_NOT_FOUND"
          error.message should include("non-existent-test")
        }
      }
    }

    "handle test not found error for incomplete test access" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )

        for
          profileResponse <- basicRequest
            .post(baseUri.addPath("api", "profiles"))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profile.id,
            "english",
            "english_basic",
            5,
            List()
          )

          testResponse <- basicRequest
            .post(baseUri.addPath("api", "tests"))
            .body(testRequest)
            .response(asJson[TestResponse])
            .send(backend)

          test = testResponse.body.toOption.get

          getResponse <- basicRequest
            .get(baseUri.addPath("api", "tests", test.testId))
            .response(asString)
            .send(backend)
        yield
          getResponse.code shouldBe StatusCode.NotFound

          val errorBody = getResponse.body match
            case Left(errorString) => errorString
            case Right(bodyString) => bodyString
          val error = readFromString[ErrorResponse](errorBody)
          error.code shouldBe "TEST_NOT_FOUND"
      }
    }

    "handle language not supported error" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )

        for
          profileResponse <- basicRequest
            .post(baseUri.addPath("api", "profiles"))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profileId = profile.id,
            language = "unsupported-language",
            dictionaryName = "english_basic",
            wordCount = 10,
            modifiers = List()
          )

          testResponse <- basicRequest
            .post(baseUri.addPath("api", "tests"))
            .body(testRequest)
            .response(asString)
            .send(backend)
        yield
          testResponse.code shouldBe StatusCode.NotFound

          val errorBody = testResponse.body match
            case Left(errorString) => errorString
            case Right(bodyString) => bodyString
          val error = readFromString[ErrorResponse](errorBody)
          error.code shouldBe "DICTIONARY_NOT_FOUND"
          error.message should include("unsupported-language")
      }
    }

    "handle invalid modifier error" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )

        for
          profileResponse <- basicRequest
            .post(baseUri.addPath("api", "profiles"))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profileId = profile.id,
            language = "english",
            dictionaryName = "english_basic",
            wordCount = 10,
            modifiers = List("invalid-modifier")
          )

          testResponse <- basicRequest
            .post(baseUri.addPath("api", "tests"))
            .body(testRequest)
            .response(asString)
            .send(backend)
        yield
          testResponse.code shouldBe StatusCode.UnprocessableEntity

          val errorBody = testResponse.body match
            case Left(errorString) => errorString
            case Right(bodyString) => bodyString
          val error = readFromString[ErrorResponse](errorBody)
          error.code shouldBe "INVALID_MODIFIER"
          error.message should include("invalid-modifier")
      }
    }

    "handle test already completed error" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )

        for
          profileResponse <- basicRequest
            .post(baseUri.addPath("api", "profiles"))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profile.id,
            "english",
            "english_basic",
            5,
            List()
          )

          testResponse <- basicRequest
            .post(baseUri.addPath("api", "tests"))
            .body(testRequest)
            .response(asJson[TestResponse])
            .send(backend)

          test = testResponse.body.toOption.get

          resultsRequest = TestResultsRequest(90.0, 87.0, 40000L, 1, List(2))

          _ <- basicRequest
            .put(baseUri.addPath("api", "tests", test.testId, "results"))
            .body(resultsRequest)
            .response(asJson[TestResponse])
            .send(backend)

          secondResultsResponse <- basicRequest
            .put(baseUri.addPath("api", "tests", test.testId, "results"))
            .body(resultsRequest)
            .response(asString)
            .send(backend)
        yield
          secondResultsResponse.code shouldBe StatusCode.BadRequest

          val errorBody = secondResultsResponse.body match
            case Left(errorString) => errorString
            case Right(bodyString) => bodyString
          val error = readFromString[ErrorResponse](errorBody)
          error.code shouldBe "TEST_ALREADY_COMPLETED"
      }
    }

    "handle no last test found error" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )

        for
          profileResponse <- basicRequest
            .post(baseUri.addPath("api", "profiles"))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          lastTestResponse <- basicRequest
            .get(baseUri.addPath("api", "profiles", profile.id, "last-test"))
            .response(asString)
            .send(backend)
        yield
          lastTestResponse.code shouldBe StatusCode.NotFound

          val errorBody = lastTestResponse.body match
            case Left(errorString) => errorString
            case Right(bodyString) => bodyString
          val error = readFromString[ErrorResponse](errorBody)
          error.code shouldBe "TEST_NOT_FOUND"
      }
    }

    "test multiple valid modifiers" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )

        for
          profileResponse <- basicRequest
            .post(baseUri.addPath("api", "profiles"))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profileId = profile.id,
            language = "english",
            dictionaryName = "english_basic",
            wordCount = 5,
            modifiers = List("uppercase", "trim", "reverse")
          )

          testResponse <- basicRequest
            .post(baseUri.addPath("api", "tests"))
            .body(testRequest)
            .response(asJson[TestResponse])
            .send(backend)
        yield
          testResponse.code shouldBe StatusCode.Created
          testResponse.body.isRight shouldBe true

          val test = testResponse.body.toOption.get
          test.modifiers should contain allElementsOf List(
            "uppercase",
            "trim",
            "reverse"
          )
          test.words should have size 5
      }
    }

    "handle malformed JSON request" in {
      withServer { backend =>
        val response = basicRequest
          .post(baseUri.addPath("api", "profiles"))
          .body("{invalid json}")
          .header("Content-Type", "application/json")
          .response(asString)
          .send(backend)

        response.map { result =>
          result.code shouldBe StatusCode.BadRequest
        }
      }
    }

    "return 404 for non-existent endpoints" in {
      withServer { backend =>
        val response = basicRequest
          .get(baseUri.addPath("api", "non-existent"))
          .response(asString)
          .send(backend)

        response.map { result =>
          result.code shouldBe StatusCode.NotFound
        }
      }
    }

    "get all available dictionaries" in {
      withServer { backend =>
        val response = basicRequest
          .get(baseUri.addPath("api", "dictionaries"))
          .response(asJson[DictionariesResponse])
          .send(backend)

        response.map { result =>
          result.code shouldBe StatusCode.Ok
          result.body.isRight shouldBe true

          val dictionaries = result.body.toOption.get
          dictionaries.dictionaries should have size 2
          dictionaries.dictionaries.map(
            _.name
          ) should contain allElementsOf List("english_basic", "spanish_basic")
          dictionaries.dictionaries.map(_.language).toSet shouldBe Set(
            "english",
            "spanish"
          )
        }
      }
    }

    "get all available languages" in {
      withServer { backend =>
        val response = basicRequest
          .get(baseUri.addPath("api", "languages"))
          .response(asJson[LanguagesResponse])
          .send(backend)

        response.map { result =>
          result.code shouldBe StatusCode.Ok
          result.body.isRight shouldBe true

          val languages = result.body.toOption.get
          languages.languages should have size 2
          languages.languages.map(_.language) should contain allElementsOf List(
            "english",
            "spanish"
          )

          val englishLang =
            languages.languages.find(_.language == "english").get
          englishLang.dictionaries should contain("english_basic")

          val spanishLang =
            languages.languages.find(_.language == "spanish").get
          spanishLang.dictionaries should contain("spanish_basic")
        }
      }
    }

    "get dictionaries for a specific language" in {
      withServer { backend =>
        val response = basicRequest
          .get(baseUri.addPath("api", "languages", "english", "dictionaries"))
          .response(asJson[DictionariesResponse])
          .send(backend)

        response.map { result =>
          result.code shouldBe StatusCode.Ok
          result.body.isRight shouldBe true

          val dictionaries = result.body.toOption.get
          dictionaries.dictionaries should have size 1
          dictionaries.dictionaries.head.name shouldBe "english_basic"
          dictionaries.dictionaries.head.language shouldBe "english"
        }
      }
    }

    "return empty list for language with no dictionaries" in {
      withServer { backend =>
        val response = basicRequest
          .get(baseUri.addPath("api", "languages", "italian", "dictionaries"))
          .response(asJson[DictionariesResponse])
          .send(backend)

        response.map { result =>
          result.code shouldBe StatusCode.Ok
          result.body.isRight shouldBe true

          val dictionaries = result.body.toOption.get
          dictionaries.dictionaries shouldBe empty
        }
      }
    }
  }

package api.integration

import analytics.calculator.AnalyticsCalculatorImpl
import analytics.repository.MongoStatisticsRepository
import analytics.repository.InMemoryStatisticsRepository
import common.DatabaseInfos
import analytics.repository.StatisticsRepository
import api.controllers.analytics.AnalyticsController
import api.controllers.config.ConfigurationController
import api.controllers.stats.StatsController
import api.controllers.typingtest.TypingTestController
import api.controllers.users.UsersController
import api.models.*
import api.models.analytics.AnalyticsModels.given
import api.models.analytics.AnalyticsResponse
import api.models.common.CommonModels.given
import common.{CommonModels, ErrorResponse}
import api.models.typingtest.TypingTestModels.given
import api.models.typingtest.*
import api.models.users.UsersModels.given
import api.models.users.{
  CreateProfileRequest,
  ProfileListResponse,
  ProfileResponse
}
import api.server.ApiServer
import api.services.{
  AnalyticsService,
  ConfigurationService,
  ProfileService,
  StatisticsService,
  TypingTestService
}
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import com.github.plokhotnyuk.jsoniter_scala.core.*
import org.http4s.blaze.client.BlazeClientBuilder
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import sttp.client3.*
import sttp.client3.jsoniter.*
import sttp.model.StatusCode
import sttp.client3.http4s.Http4sBackend
import sttp.model.Uri
import typingTest.dictionary.model.DictionaryJson
import typingTest.dictionary.repository.{
  DictionaryRepository,
  FileDictionaryRepository
}
import typingTest.tests.repository.{
  InMemoryTypingTestRepository,
  MongoTypingTestRepository,
  TypingTestRepository
}
import users_management.repository.{
  InMemoryProfileRepository,
  MongoProfileRepository,
  ProfileRepository
}
import org.testcontainers.containers.MongoDBContainer
import org.testcontainers.utility.DockerImageName

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

  private val useMongoDb = true
  private var mongoContainer: MongoDBContainer = _
  private var profileRepository: ProfileRepository = uninitialized
  private var typingTestRepository: TypingTestRepository = uninitialized
  private var dictionaryRepository: DictionaryRepository = uninitialized
  private var statisticsRepository: StatisticsRepository = uninitialized
  private var tempDir: File = uninitialized
  private var testPort: Int = uninitialized
  private var baseUri: Uri = uninitialized

  override def beforeEach(): Unit =
    if useMongoDb then
      mongoContainer = MongoDBContainer(DockerImageName.parse("mongo:4.4"))
      mongoContainer.start()
      val dbInfosProfiles =
        DatabaseInfos("profiles", mongoContainer.getReplicaSetUrl, "test_db")
      val dbInfosTyping = DatabaseInfos(
        "typing_tests",
        mongoContainer.getReplicaSetUrl,
        "test_db"
      )
      val dbInfosStats =
        DatabaseInfos("statistics", mongoContainer.getReplicaSetUrl, "test_db")
      profileRepository = MongoProfileRepository(dbInfosProfiles)
      typingTestRepository = MongoTypingTestRepository(dbInfosTyping)
      statisticsRepository = MongoStatisticsRepository(dbInfosStats)
    else
      profileRepository = InMemoryProfileRepository()
      typingTestRepository = InMemoryTypingTestRepository()
      statisticsRepository = InMemoryStatisticsRepository()

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

    try
      if profileRepository != null then profileRepository.close()
      if typingTestRepository != null then typingTestRepository.close()
      if statisticsRepository != null then statisticsRepository.close()
    catch
      case e: Exception =>
        println(s"Error closing repositories: ${e.getMessage}")

    if useMongoDb && mongoContainer != null then
      try mongoContainer.stop()
      catch
        case e: Exception =>
          println(s"Error stopping MongoDB container: ${e.getMessage}")

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

  private def writeToString(dict: DictionaryJson): String =
    com.github.plokhotnyuk.jsoniter_scala.core.writeToString(dict)

  private def withServer[T](test: SttpBackend[IO, Any] => IO[T]): IO[T] =
    val typingTestService = TypingTestService(
      profileRepository,
      dictionaryRepository,
      typingTestRepository,
      statisticsRepository
    )
    val statisticsService = StatisticsService(statisticsRepository)
    val analyticsService =
      AnalyticsService(statisticsService, AnalyticsCalculatorImpl())

    val testConfig = _root_.config.AppConfig(
      database = _root_.config.DatabaseConfig(
        mongoUri = if useMongoDb then mongoContainer.getReplicaSetUrl else "",
        databaseName = "test_db",
        useMongoDb = useMongoDb
      ),
      server = _root_.config.ServerConfig(
        host = "localhost",
        port = testPort,
        threadPool = _root_.config.ThreadPoolConfig(
          coreSize = 2,
          maxSize = 4,
          keepAliveSeconds = 30,
          queueSize = 100,
          threadNamePrefix = "test-api"
        )
      ),
      dictionary = _root_.config.DictionaryConfig(
        basePath = "src/test/resources/dictionaries",
        autoCreateDirectories = true
      ),
      version = "1.0.0"
    )

    val configServiceIO = ConfigurationService.create(
      testConfig,
      profileRepository,
      typingTestRepository,
      dictionaryRepository,
      statisticsRepository
    )

    configServiceIO.flatMap { configService =>
      val profileService = ProfileService(profileRepository)
      val usersController = UsersController(profileService)
      val typingTestController = TypingTestController(typingTestService)
      val statsController = StatsController(statisticsService)
      val analyticsController = AnalyticsController(analyticsService)
      val configController = ConfigurationController(configService)

      val server = ApiServer(
        usersController,
        typingTestController,
        statsController,
        analyticsController,
        configController,
        testConfig
      )

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

        basicRequest
          .post(baseUri.addPath(Seq("api", "profiles")))
          .body(request)
          .response(asJson[ProfileResponse])
          .send(backend)
          .map { result =>
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
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(createRequest)
            .response(asJson[ProfileResponse])
            .send(backend)
          _ <- basicRequest
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(
              createRequest.copy(name = "Test User 2", email = "user2@test.com")
            )
            .response(asJson[ProfileResponse])
            .send(backend)

          listResponse <- basicRequest
            .get(baseUri.addPath(Seq("api", "profiles")))
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
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profileId = profile.id,
            sources = List(SourceWithMerger("english_basic")),
            wordCount = 10,
            modifiers = List("lowercase")
          )

          testResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
            .body(testRequest)
            .response(asJson[TestResponse])
            .send(backend)
        yield
          testResponse.code shouldBe StatusCode.Created
          testResponse.body.isRight shouldBe true

          val test = testResponse.body.toOption.get
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
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profileId = profile.id,
            sources = List(SourceWithMerger("english_basic")),
            wordCount = 10,
            modifiers = List("lowercase"),
            timeLimit = Some(60000L)
          )

          testResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
            .body(testRequest)
            .response(asJson[TestResponse])
            .send(backend)
        yield
          testResponse.code shouldBe StatusCode.Created
          testResponse.body.isRight shouldBe true

          val test = testResponse.body.toOption.get
          test.testId should not be empty
          test.completedAt shouldBe None
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
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profileId = profile.id,
            sources = List(SourceWithMerger("english_basic")),
            wordCount = 5,
            modifiers = List(),
            timeLimit = Some(30000L)
          )

          _ <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
            .body(testRequest)
            .response(asJson[TestResponse])
            .send(backend)

          lastTestResponse <- basicRequest
            .get(
              baseUri.addPath(Seq("api", "profiles", profile.id, "last-test"))
            )
            .response(asJson[LastTestResponse])
            .send(backend)
        yield
          lastTestResponse.code shouldBe StatusCode.Ok
          lastTestResponse.body.isRight shouldBe true

          val lastTest = lastTestResponse.body.toOption.get
          lastTest.words should have size 5
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
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profileId = profile.id,
            sources = List(SourceWithMerger("english_basic")),
            wordCount = 5,
            modifiers = List()
          )

          testResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
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
            .put(baseUri.addPath(Seq("api", "tests", test.testId, "results")))
            .body(resultsRequest)
            .response(asJson[TestResponse])
            .send(backend)
        yield
          resultsResponse.code shouldBe StatusCode.Ok
          resultsResponse.body.isRight shouldBe true

          val completedTest = resultsResponse.body.toOption.get
          completedTest.testId shouldBe test.testId
          println(completedTest)
          completedTest.completedAt shouldBe defined
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
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profileId = profile.id,
            sources = List(SourceWithMerger("english_basic")),
            wordCount = 5,
            modifiers = List()
          )

          testResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
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
            .put(baseUri.addPath(Seq("api", "tests", test.testId, "results")))
            .body(resultsRequest)
            .response(asJson[TestResponse])
            .send(backend)

          getResponse <- basicRequest
            .get(baseUri.addPath(Seq("api", "tests", test.testId)))
            .response(asJson[TestResponse])
            .send(backend)
        yield
          getResponse.code shouldBe StatusCode.Ok
          getResponse.body.isRight shouldBe true

          val persistedTest = getResponse.body.toOption.get
          persistedTest.testId shouldBe test.testId
          persistedTest.profileId shouldBe profile.id
          persistedTest.completedAt shouldBe defined
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
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest1 = TestRequest(
            profile.id,
            List(SourceWithMerger("english_basic")),
            5,
            List("lowercase")
          )
          testRequest2 = TestRequest(
            profile.id,
            List(SourceWithMerger("spanish_basic")),
            3,
            List("uppercase")
          )

          testResponse1 <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
            .body(testRequest1)
            .response(asJson[TestResponse])
            .send(backend)

          resultsRequest = TestResultsRequest(85.0, 82.0, 40000L, 1, List(2))

          _ <- basicRequest
            .put(
              baseUri.addPath(
                Seq(
                  "api",
                  "tests",
                  testResponse1.body.toOption.get.testId,
                  "results"
                )
              )
            )
            .body(resultsRequest)
            .response(asJson[TestResponse])
            .send(backend)

          testResponse2 <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
            .body(testRequest2)
            .response(asJson[TestResponse])
            .send(backend)

          _ <- basicRequest
            .put(
              baseUri.addPath(
                Seq(
                  "api",
                  "tests",
                  testResponse2.body.toOption.get.testId,
                  "results"
                )
              )
            )
            .body(resultsRequest)
            .response(asJson[TestResponse])
            .send(backend)

          testsResponse <- basicRequest
            .get(baseUri.addPath(Seq("api", "profiles", profile.id, "tests")))
            .response(asJson[TestListResponse])
            .send(backend)
        yield
          testsResponse.code shouldBe StatusCode.Ok
          testsResponse.body.isRight shouldBe true

          val testList = testsResponse.body.toOption.get
          testList.tests should have size 2
          testList.tests.foreach(_.profileId shouldBe profile.id)
          testList.tests.flatMap(_.sources) should contain allElementsOf List(
            SourceWithMerger("english_basic"),
            SourceWithMerger("spanish_basic")
          )
          testList.tests.foreach(_.completedAt shouldBe defined)
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
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest1 = TestRequest(
            profile.id,
            List(SourceWithMerger("english_basic")),
            5,
            List()
          )
          testRequest2 = TestRequest(
            profile.id,
            List(SourceWithMerger("english_basic")),
            3,
            List()
          )

          _ <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
            .body(testRequest1)
            .response(asJson[TestResponse])
            .send(backend)

          _ <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
            .body(testRequest2)
            .response(asJson[TestResponse])
            .send(backend)

          lastTestResponse <- basicRequest
            .get(
              baseUri.addPath(Seq("api", "profiles", profile.id, "last-test"))
            )
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
          sources = List(SourceWithMerger("english_basic")),
          wordCount = 10,
          modifiers = List()
        )

        val response = basicRequest
          .post(baseUri.addPath(Seq("api", "tests")))
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
          .get(baseUri.addPath(Seq("api", "tests", "non-existent-test")))
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
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profile.id,
            List(SourceWithMerger("english_basic")),
            5,
            List()
          )

          testResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
            .body(testRequest)
            .response(asJson[TestResponse])
            .send(backend)

          test = testResponse.body.toOption.get

          getResponse <- basicRequest
            .get(baseUri.addPath(Seq("api", "tests", test.testId)))
            .response(asString)
            .send(backend)
        yield
          getResponse.code shouldBe StatusCode.NotFound

          val errorBody = getResponse.body match
            case Left(errorString) => errorString
            case Right(bodyString) => bodyString
          val error = readFromString[ErrorResponse](errorBody)
          error.code shouldBe "TEST_NOT_FOUND"
          ()
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
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profileId = profile.id,
            sources = List(SourceWithMerger("unsupported-source")),
            wordCount = 10,
            modifiers = List()
          )

          testResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
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
          error.message should include("unsupported-source")
          ()
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
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profileId = profile.id,
            sources = List(SourceWithMerger("english_basic")),
            wordCount = 10,
            modifiers = List("invalid-modifier")
          )

          testResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
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
          ()
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
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profile.id,
            List(SourceWithMerger("english_basic")),
            5,
            List()
          )

          testResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
            .body(testRequest)
            .response(asJson[TestResponse])
            .send(backend)

          test = testResponse.body.toOption.get

          resultsRequest = TestResultsRequest(90.0, 87.0, 40000L, 1, List(2))

          _ <- basicRequest
            .put(baseUri.addPath(Seq("api", "tests", test.testId, "results")))
            .body(resultsRequest)
            .response(asJson[TestResponse])
            .send(backend)

          secondResultsResponse <- basicRequest
            .put(baseUri.addPath(Seq("api", "tests", test.testId, "results")))
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
          ()
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
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          lastTestResponse <- basicRequest
            .get(
              baseUri.addPath(Seq("api", "profiles", profile.id, "last-test"))
            )
            .response(asString)
            .send(backend)
        yield
          lastTestResponse.code shouldBe StatusCode.NotFound

          val errorBody = lastTestResponse.body match
            case Left(errorString) => errorString
            case Right(bodyString) => bodyString
          val error = readFromString[ErrorResponse](errorBody)
          error.code shouldBe "TEST_NOT_FOUND"
          ()
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
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profileId = profile.id,
            sources = List(SourceWithMerger("english_basic")),
            wordCount = 5,
            modifiers = List("uppercase", "trim", "reverse")
          )

          testResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
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
          ()
      }
    }

    "handle malformed JSON request" in {
      withServer { backend =>
        basicRequest
          .post(baseUri.addPath(Seq("api", "profiles")))
          .body("{invalid json}")
          .header("Content-Type", "application/json")
          .response(asString)
          .send(backend)
          .map { result =>
            result.code shouldBe StatusCode.BadRequest
            ()
          }
      }
    }

    "return 404 for non-existent endpoints" in {
      withServer { backend =>
        basicRequest
          .get(baseUri.addPath(Seq("api", "non-existent")))
          .response(asString)
          .send(backend)
          .map { result =>
            result.code shouldBe StatusCode.NotFound
            ()
          }
      }
    }

    "get all available dictionaries" in {
      withServer { backend =>
        basicRequest
          .get(baseUri.addPath(Seq("api", "dictionaries")))
          .response(asJson[DictionariesResponse])
          .send(backend)
          .map { result =>
            result.code shouldBe StatusCode.Ok
            result.body.isRight shouldBe true

            val dictionaries = result.body.toOption.get
            dictionaries.dictionaries should have size 2
            dictionaries.dictionaries.map(
              _.name
            ) should contain allElementsOf List(
              "english_basic",
              "spanish_basic"
            )
            ()
          }
      }
    }

    "create test with two sources and concatenate merger" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )
        for
          profileResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)
          profile = profileResponse.body.toOption.get
          testRequest = TestRequest(
            profileId = profile.id,
            sources = List(
              SourceWithMerger("english_basic"),
              SourceWithMerger("spanish_basic", Some("concatenate"))
            ),
            wordCount = 30,
            modifiers = List("lowercase")
          )
          testResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
            .body(testRequest)
            .response(asJson[TestResponse])
            .send(backend)
        yield
          testResponse.code shouldBe StatusCode.Created
          testResponse.body.isRight shouldBe true
          val test = testResponse.body.toOption.get
          test.sources should have size 2
          test.sources.map(_.name) should contain allElementsOf List(
            "english_basic",
            "spanish_basic"
          )
          test.words.size should be <= 30
      }
    }

    "ignore the second source if a merger is missing for the second source" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )
        for
          profileResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)
          profile = profileResponse.body.toOption.get
          testRequest = TestRequest(
            profileId = profile.id,
            sources = List(
              SourceWithMerger("english_basic"),
              SourceWithMerger("spanish_basic")
            ),
            wordCount = 10
          )
          testResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
            .body(testRequest)
            .response(asJson[TestResponse])
            .send(backend)
        yield
          val spanish = Seq(
            "hola",
            "mundo",
            "prueba",
            "escribir",
            "teclado",
            "codigo",
            "programacion",
            "desarrollador",
            "ordenador",
            "raton",
            "pantalla",
            "archivo",
            "directorio",
            "carpeta"
          )
          testResponse.body.isRight shouldBe true
          testResponse.body.toOption.get.words
            .filter(spanish.contains) shouldBe empty

      }
    }

    "fail if an invalid merger name is provided" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )
        for
          profileResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)
          profile = profileResponse.body.toOption.get
          testRequest = TestRequest(
            profileId = profile.id,
            sources = List(
              SourceWithMerger("english_basic"),
              SourceWithMerger("spanish_basic", Some("notamerger"))
            ),
            wordCount = 10
          )
          testResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
            .body(testRequest)
            .response(asString)
            .send(backend)
        yield testResponse.code shouldBe StatusCode.UnprocessableEntity
      }
    }

    "merge words using alternate merger" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )
        for
          profileResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)
          profile = profileResponse.body.toOption.get
          testRequest = TestRequest(
            profileId = profile.id,
            sources = List(
              SourceWithMerger("english_basic"),
              SourceWithMerger("spanish_basic", Some("alternate"))
            ),
            wordCount = 10
          )
          testResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
            .body(testRequest)
            .response(asJson[TestResponse])
            .send(backend)
        yield
          testResponse.code shouldBe StatusCode.Created
          testResponse.body.isRight shouldBe true
          val test = testResponse.body.toOption.get
          test.words.size shouldBe 10
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
          print(test.words)
          val alternated =
            test.words.take(10).zipWithIndex.forall { case (w, i) =>
              if i % 2 == 0 then englishWords.contains(w)
              else spanishWords.contains(w)
            }
          alternated should be(true)
      }
    }

    "get all available modifiers" in {
      withServer { backend =>
        basicRequest
          .get(baseUri.addPath(Seq("api", "modifiers")))
          .response(asJson[ModifiersResponse])
          .send(backend)
          .map { result =>
            result.code shouldBe StatusCode.Ok
            result.body.isRight shouldBe true

            val modifiers = result.body.toOption.get
            modifiers.modifiers should not be empty
            modifiers.modifiers.foreach { modifier =>
              modifier.name should not be empty
              modifier.description should not be empty
            }
          }
      }
    }

    "get all available mergers" in {
      withServer { backend =>
        basicRequest
          .get(baseUri.addPath(Seq("api", "mergers")))
          .response(asJson[MergersResponse])
          .send(backend)
          .map { result =>
            result.code shouldBe StatusCode.Ok
            result.body.isRight shouldBe true

            val mergers = result.body.toOption.get
            mergers.mergers should not be empty
            mergers.mergers.foreach { merger =>
              merger.name should not be empty
              merger.description should not be empty
            }
          }
      }
    }

    "get user analytics with errors" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )

        for
          profileResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest1 = TestRequest(
            profileId = profile.id,
            sources = List(SourceWithMerger("english_basic")),
            wordCount = 5
          )

          testResponse1 <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
            .body(testRequest1)
            .response(asJson[TestResponse])
            .send(backend)

          test1 = testResponse1.body.toOption.get

          resultsRequest1 = TestResultsRequest(
            accuracy = 90.0,
            rawAccuracy = 87.0,
            testTime = 40000L,
            errorCount = 2,
            errorWordIndices = List(1, 3)
          )

          _ <- basicRequest
            .put(baseUri.addPath(Seq("api", "tests", test1.testId, "results")))
            .body(resultsRequest1)
            .response(asJson[TestResponse])
            .send(backend)

          testRequest2 = TestRequest(
            profileId = profile.id,
            sources = List(SourceWithMerger("english_basic")),
            wordCount = 5
          )

          testResponse2 <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
            .body(testRequest2)
            .response(asJson[TestResponse])
            .send(backend)

          test2 = testResponse2.body.toOption.get

          resultsRequest2 = TestResultsRequest(
            accuracy = 95.0,
            rawAccuracy = 93.0,
            testTime = 35000L,
            errorCount = 1,
            errorWordIndices = List(2)
          )

          _ <- basicRequest
            .put(baseUri.addPath(Seq("api", "tests", test2.testId, "results")))
            .body(resultsRequest2)
            .response(asJson[TestResponse])
            .send(backend)

          analyticsResponse <- basicRequest
            .get(baseUri.addPath(Seq("api", "analytics", profile.id)))
            .response(asJson[AnalyticsResponse])
            .send(backend)
        yield
          analyticsResponse.code shouldBe StatusCode.Ok
          analyticsResponse.body.isRight shouldBe true

          val analytics = analyticsResponse.body.toOption.get
          analytics.userId shouldBe profile.id
          analytics.totalTests shouldBe 2
          analytics.totalErrors shouldBe 3
          analytics.averageErrorsPerTest shouldBe 1.5
          analytics.averageWpm should be > 0.0
          analytics.averageAccuracy should be > 0.0
      }
    }

    "get user analytics with no errors" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )

        for
          profileResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          testRequest = TestRequest(
            profileId = profile.id,
            sources = List(SourceWithMerger("english_basic")),
            wordCount = 5
          )

          testResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "tests")))
            .body(testRequest)
            .response(asJson[TestResponse])
            .send(backend)

          test = testResponse.body.toOption.get

          resultsRequest = TestResultsRequest(
            accuracy = 100.0,
            rawAccuracy = 100.0,
            testTime = 40000L,
            errorCount = 0,
            errorWordIndices = List.empty
          )

          _ <- basicRequest
            .put(baseUri.addPath(Seq("api", "tests", test.testId, "results")))
            .body(resultsRequest)
            .response(asJson[TestResponse])
            .send(backend)

          analyticsResponse <- basicRequest
            .get(baseUri.addPath(Seq("api", "analytics", profile.id)))
            .response(asJson[api.models.analytics.AnalyticsResponse])
            .send(backend)
        yield
          analyticsResponse.code shouldBe StatusCode.Ok
          analyticsResponse.body.isRight shouldBe true

          val analytics = analyticsResponse.body.toOption.get
          analytics.userId shouldBe profile.id
          analytics.totalTests shouldBe 1
          analytics.totalErrors shouldBe 0
          analytics.averageErrorsPerTest shouldBe 0.0
          analytics.averageWpm should be > 0.0
          analytics.averageAccuracy shouldBe 100.0
      }
    }

    "get user analytics for user with no tests" in {
      withServer { backend =>
        val profileRequest = CreateProfileRequest(
          name = "Test User",
          email = "test@example.com",
          settings = Set.empty
        )

        for
          profileResponse <- basicRequest
            .post(baseUri.addPath(Seq("api", "profiles")))
            .body(profileRequest)
            .response(asJson[ProfileResponse])
            .send(backend)

          profile = profileResponse.body.toOption.get

          analyticsResponse <- basicRequest
            .get(baseUri.addPath(Seq("api", "analytics", profile.id)))
            .response(asJson[api.models.analytics.AnalyticsResponse])
            .send(backend)
        yield
          analyticsResponse.code shouldBe StatusCode.Ok
          analyticsResponse.body.isRight shouldBe true

          val analytics = analyticsResponse.body.toOption.get
          analytics.userId shouldBe profile.id
          analytics.totalTests shouldBe 0
          analytics.totalErrors shouldBe 0
          analytics.averageErrorsPerTest shouldBe 0.0
          analytics.averageWpm shouldBe 0.0
          analytics.averageAccuracy shouldBe 0.0
      }
    }
  }

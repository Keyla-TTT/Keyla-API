package config

import analytics.repository.InMemoryStatisticsRepository
import analytics.calculator.AnalyticsCalculatorImpl
import api.controllers.analytics.AnalyticsController
import api.controllers.stats.StatsController
import api.controllers.typingtest.TypingTestController
import api.controllers.users.UsersController
import api.models.*
import api.models.ApiModels.given
import api.server.ApiServer
import api.services.{
  AnalyticsService,
  ConfigEntry,
  ConfigListResponse,
  ConfigUpdateResponse,
  ConfigurationService,
  ProfileService,
  SimpleConfigUpdateRequest,
  StatisticsService,
  TypingTestService
}
import api.controllers.config.ConfigurationController
import cats.effect.IO
import cats.effect.testing.scalatest.AsyncIOSpec
import org.http4s.blaze.client.BlazeClientBuilder
import org.scalatest.BeforeAndAfterEach
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AsyncWordSpec
import sttp.client3.*
import sttp.client3.http4s.Http4sBackend
import sttp.client3.jsoniter.*
import sttp.model.{StatusCode, Uri}
import typingTest.dictionary.repository.FileDictionaryRepository
import typingTest.tests.repository.InMemoryTypingTestRepository
import users_management.repository.InMemoryProfileRepository
import users_management.service.ProfileService

import java.io.File

class ConfigurationIntegrationSpec
    extends AsyncWordSpec
    with AsyncIOSpec
    with Matchers
    with BeforeAndAfterEach:

  private val testConfigPath = "test-keyla-config.json"
  private val testHost = "localhost"
  private var testPort = 9999

  override def beforeEach(): Unit =
    super.beforeEach()
    testPort = testPort + 1
    val configFile = new File(testConfigPath)
    if configFile.exists() then configFile.delete()

  override def afterEach(): Unit =
    super.afterEach()
    val configFile = new File(testConfigPath)
    if configFile.exists() then configFile.delete()

  private def getBaseUri(): Uri =
    Uri
      .parse(s"http://$testHost:$testPort")
      .getOrElse(throw new RuntimeException("Invalid URI"))

  private def createTestServer(): IO[ApiServer] =
    for
      testConfig <- IO.pure(
        AppConfig(
          database = DatabaseConfig(
            mongoUri = "mongodb://test:27017",
            databaseName = "test_db",
            useMongoDb = false
          ),
          server = ServerConfig(
            host = testHost,
            port = testPort,
            threadPool = ThreadPoolConfig(
              coreSize = 2,
              maxSize = 4,
              keepAliveSeconds = 30,
              queueSize = 100,
              threadNamePrefix = "test-api"
            )
          ),
          dictionary = DictionaryConfig(
            basePath = "src/test/resources/dictionaries",
            autoCreateDirectories = true
          ),
          version = "1.0.0"
        )
      )

      profileRepository <- IO.pure(InMemoryProfileRepository())
      dictionaryRepository <- IO.pure(
        FileDictionaryRepository(
          testConfig.dictionary.basePath
        )
      )
      typingTestRepository <- IO.pure(InMemoryTypingTestRepository())
      analyticsRepository <- IO.pure(InMemoryStatisticsRepository())

      configService <- ConfigurationService.create(
        testConfig,
        profileRepository,
        typingTestRepository,
        dictionaryRepository,
        analyticsRepository
      )

      typingTestService <- IO.pure(
        TypingTestService(
          profileRepository,
          dictionaryRepository,
          typingTestRepository,
          analyticsRepository
        )
      )

      statisticsService <- IO.pure(
        StatisticsService(analyticsRepository)
      )
      analyticsService <- IO.pure(
        AnalyticsService(statisticsService, AnalyticsCalculatorImpl())
      )

      profileService <- IO.pure(ProfileService(profileRepository))
      usersController <- IO.pure(UsersController(profileService))
      typingTestController <- IO.pure(TypingTestController(typingTestService))
      statsController <- IO.pure(StatsController(statisticsService))
      analyticsController <- IO.pure(
        AnalyticsController(analyticsService)
      )
      configController <- IO.pure(ConfigurationController(configService))

      server <- IO.pure(
        ApiServer(
          usersController,
          typingTestController,
          statsController,
          analyticsController,
          configController,
          testConfig
        )
      )
    yield server

  "Configuration API" should {

    "get all configuration entries" in {
      createTestServer().flatMap { server =>
        server.resource(testPort, testHost).use { _ =>
          BlazeClientBuilder[IO].resource.use { client =>
            val backend = Http4sBackend.usingClient(client)
            val request = basicRequest
              .get(getBaseUri().addPath("api", "config"))
              .response(asJson[ConfigListResponse])

            for
              response <- backend.send(request)
              _ = response.code shouldBe StatusCode.Ok
              configList = response.body match
                case Right(value) => value
                case Left(error) =>
                  throw new RuntimeException(
                    s"Failed to parse response: $error"
                  )
              _ = configList.entries should not be empty
              _ = configList.entries.map(_.key.section) should contain allOf (
                "database",
                "server",
                "dictionary"
              )
            yield ()
          }
        }
      }
    }

    "get a specific configuration entry" in {
      createTestServer().flatMap { server =>
        server.resource(testPort, testHost).use { _ =>
          BlazeClientBuilder[IO].resource.use { client =>
            val backend = Http4sBackend.usingClient(client)
            val request = basicRequest
              .get(getBaseUri().addPath("api", "config", "database.mongoUri"))
              .response(asJson[ConfigEntry])

            for
              response <- backend.send(request)
              _ = response.code shouldBe StatusCode.Ok
              configEntry = response.body match
                case Right(value) => value
                case Left(error) =>
                  throw new RuntimeException(
                    s"Failed to parse response: $error"
                  )
              _ = configEntry.key.section shouldBe "database"
              _ = configEntry.key.key shouldBe "mongoUri"
              _ = configEntry.value shouldBe "mongodb://test:27017"
              _ = configEntry.dataType shouldBe "string"
            yield ()
          }
        }
      }
    }

    "return 404 for non-existent configuration entry" in {
      createTestServer().flatMap { server =>
        server.resource(testPort, testHost).use { _ =>
          BlazeClientBuilder[IO].resource.use { client =>
            val backend = Http4sBackend.usingClient(client)
            val request = basicRequest
              .get(getBaseUri().addPath("api", "config", "nonexistent.key"))

            for
              response <- backend.send(request)
              _ = response.code shouldBe StatusCode.NotFound
            yield ()
          }
        }
      }
    }

    "update a configuration entry" in {
      createTestServer().flatMap { server =>
        server.resource(testPort, testHost).use { _ =>
          BlazeClientBuilder[IO].resource.use { client =>
            val backend = Http4sBackend.usingClient(client)

            val updateRequest = SimpleConfigUpdateRequest(
              key = "database.mongoUri",
              value = "mongodb://updated:27017"
            )

            val request = basicRequest
              .put(getBaseUri().addPath("api", "config"))
              .body(updateRequest)
              .response(asJson[ConfigUpdateResponse])

            for
              response <- backend.send(request)
              _ = response.code shouldBe StatusCode.Ok
              updateResponse = response.body match
                case Right(value) => value
                case Left(error) =>
                  throw new RuntimeException(
                    s"Failed to parse response: $error"
                  )
              _ = updateResponse.success shouldBe true
              _ = updateResponse.message should include("reinitialized")
            yield ()
          }
        }
      }
    }

    "return 400 for invalid configuration value" in {
      createTestServer().flatMap { server =>
        server.resource(testPort, testHost).use { _ =>
          BlazeClientBuilder[IO].resource.use { client =>
            val backend = Http4sBackend.usingClient(client)

            val updateRequest = SimpleConfigUpdateRequest(
              key = "server.port",
              value = "invalid_port"
            )

            val request = basicRequest
              .put(getBaseUri().addPath("api", "config"))
              .body(updateRequest)

            for
              response <- backend.send(request)
              _ = response.code shouldBe StatusCode.BadRequest
            yield ()
          }
        }
      }
    }

    "get current complete configuration" in {
      createTestServer().flatMap { server =>
        server.resource(testPort, testHost).use { _ =>
          BlazeClientBuilder[IO].resource.use { client =>
            val backend = Http4sBackend.usingClient(client)
            val request = basicRequest
              .get(getBaseUri().addPath("api", "config"))
              .response(asJson[ConfigListResponse])

            for
              response <- backend.send(request)
              _ = response.code shouldBe StatusCode.Ok
              configList = response.body match
                case Right(value) => value
                case Left(error) =>
                  throw new RuntimeException(
                    s"Failed to parse response: $error"
                  )
              _ = configList.entries should not be empty

              mongoUriEntry = configList.entries
                .find(_.key.key == "mongoUri")
                .get
              _ = mongoUriEntry.value shouldBe "mongodb://test:27017"

              portEntry = configList.entries.find(_.key.key == "port").get
              _ = portEntry.value shouldBe testPort.toString

              basePathEntry = configList.entries
                .find(_.key.key == "basePath")
                .get
              _ = basePathEntry.value shouldBe "src/test/resources/dictionaries"
            yield ()
          }
        }
      }
    }

    "reset configuration to defaults" in {
      createTestServer().flatMap { server =>
        server.resource(testPort, testHost).use { _ =>
          BlazeClientBuilder[IO].resource.use { client =>
            val backend = Http4sBackend.usingClient(client)

            val updateRequest = SimpleConfigUpdateRequest(
              key = "database.mongoUri",
              value = "mongodb://modified:27017"
            )

            val updateReq = basicRequest
              .put(getBaseUri().addPath("api", "config"))
              .body(updateRequest)
              .response(asJson[ConfigUpdateResponse])

            val resetRequest = basicRequest
              .post(getBaseUri().addPath("api", "config", "reset"))
              .response(asJson[AppConfig])

            for
              _ <- backend.send(updateReq)
              resetResponse <- backend.send(resetRequest)
              _ = resetResponse.code shouldBe StatusCode.Ok
              config = resetResponse.body match
                case Right(value) => value
                case Left(error) =>
                  throw new RuntimeException(
                    s"Failed to parse response: $error"
                  )
              _ = config.database.mongoUri shouldBe "mongodb://localhost:27017"
              _ = config.server.port shouldBe 9999
            yield ()
          }
        }
      }
    }

    "handle configuration workflow like git config" in {
      createTestServer().flatMap { server =>
        server.resource(testPort, testHost).use { _ =>
          BlazeClientBuilder[IO].resource.use { client =>
            val backend = Http4sBackend.usingClient(client)

            val listRequest = basicRequest
              .get(getBaseUri().addPath("api", "config"))
              .response(asJson[ConfigListResponse])

            val getRequest = basicRequest
              .get(getBaseUri().addPath("api", "config", "database.mongoUri"))
              .response(asJson[ConfigEntry])

            val customPath = "src/test/resources/custom-dictionaries"
            val setRequest = basicRequest
              .put(getBaseUri().addPath("api", "config"))
              .body(
                SimpleConfigUpdateRequest(
                  key = "dictionary.basePath",
                  value = customPath
                )
              )
              .response(asJson[ConfigUpdateResponse])

            val verifyRequest = basicRequest
              .get(getBaseUri().addPath("api", "config", "dictionary.basePath"))
              .response(asJson[ConfigEntry])

            for
              listResponse <- backend.send(listRequest)
              _ = listResponse.code shouldBe StatusCode.Ok
              configList = listResponse.body match
                case Right(value) => value
                case Left(error) =>
                  throw new RuntimeException(
                    s"Failed to parse list response: $error"
                  )
              _ = configList.entries.length should be > 5

              getResponse <- backend.send(getRequest)
              _ = getResponse.code shouldBe StatusCode.Ok
              originalEntry = getResponse.body match
                case Right(value) => value
                case Left(error) =>
                  throw new RuntimeException(
                    s"Failed to parse get response: $error"
                  )

              setResponse <- backend.send(setRequest)
              _ = setResponse.code shouldBe StatusCode.Ok
              setResult = setResponse.body match
                case Right(value) => value
                case Left(error) =>
                  throw new RuntimeException(
                    s"Failed to parse set response: $error"
                  )
              _ = setResult.success shouldBe true
              _ = setResult.message should include("reinitialized")

              verifyResponse <- backend.send(verifyRequest)
              _ = verifyResponse.code shouldBe StatusCode.Ok
              updatedEntry = verifyResponse.body match
                case Right(value) => value
                case Left(error) =>
                  throw new RuntimeException(
                    s"Failed to parse verify response: $error"
                  )
              _ = updatedEntry.value shouldBe customPath
              _ = updatedEntry.value should not equal originalEntry.value
            yield ()
          }
        }
      }
    }

    "demonstrate dynamic repository switching" in {
      createTestServer().flatMap { server =>
        server.resource(testPort, testHost).use { _ =>
          BlazeClientBuilder[IO].resource.use { client =>
            val backend = Http4sBackend.usingClient(client)

            val enableMongoRequest = basicRequest
              .put(getBaseUri().addPath("api", "config"))
              .body(
                SimpleConfigUpdateRequest(
                  key = "database.useMongoDb",
                  value = "true"
                )
              )
              .response(asJson[ConfigUpdateResponse])

            val changeUriRequest = basicRequest
              .put(getBaseUri().addPath("api", "config"))
              .body(
                SimpleConfigUpdateRequest(
                  key = "database.mongoUri",
                  value = "mongodb://newhost:27017"
                )
              )
              .response(asJson[ConfigUpdateResponse])

            val disableMongoRequest = basicRequest
              .put(getBaseUri().addPath("api", "config"))
              .body(
                SimpleConfigUpdateRequest(
                  key = "database.useMongoDb",
                  value = "false"
                )
              )
              .response(asJson[ConfigUpdateResponse])

            for
              enableResponse <- backend.send(enableMongoRequest)
              _ = enableResponse.code shouldBe StatusCode.Ok
              enableResult = enableResponse.body match
                case Right(value) => value
                case Left(error) =>
                  throw new RuntimeException(
                    s"Failed to parse response: $error"
                  )
              _ = enableResult.message should include("reinitialized")

              uriResponse <- backend.send(changeUriRequest)
              _ = uriResponse.code shouldBe StatusCode.Ok
              uriResult = uriResponse.body match
                case Right(value) => value
                case Left(error) =>
                  throw new RuntimeException(
                    s"Failed to parse response: $error"
                  )
              _ = uriResult.message should include("reinitialized")

              disableResponse <- backend.send(disableMongoRequest)
              _ = disableResponse.code shouldBe StatusCode.Ok
              disableResult = disableResponse.body match
                case Right(value) => value
                case Left(error) =>
                  throw new RuntimeException(
                    s"Failed to parse response: $error"
                  )
              _ = disableResult.message should include("reinitialized")
            yield ()
          }
        }
      }
    }

    "indicate restart required for port configuration changes" in {
      createTestServer().flatMap { server =>
        server.resource(testPort, testHost).use { _ =>
          BlazeClientBuilder[IO].resource.use { client =>
            val backend = Http4sBackend.usingClient(client)

            val updateRequest = SimpleConfigUpdateRequest(
              key = "server.port",
              value = "9090"
            )

            val request = basicRequest
              .put(getBaseUri().addPath("api", "config"))
              .body(updateRequest)
              .response(asJson[ConfigUpdateResponse])

            for
              response <- backend.send(request)
              _ = response.code shouldBe StatusCode.Ok
              updateResponse = response.body match
                case Right(value) => value
                case Left(error) =>
                  throw new RuntimeException(
                    s"Failed to parse response: $error"
                  )
              _ = updateResponse.success shouldBe true
              _ = updateResponse.message should include("restart required")
            yield ()
          }
        }
      }
    }

    "indicate restart required for host configuration changes" in {
      createTestServer().flatMap { server =>
        server.resource(testPort, testHost).use { _ =>
          BlazeClientBuilder[IO].resource.use { client =>
            val backend = Http4sBackend.usingClient(client)

            val updateRequest = SimpleConfigUpdateRequest(
              key = "server.host",
              value = "0.0.0.0"
            )

            val request = basicRequest
              .put(getBaseUri().addPath("api", "config"))
              .body(updateRequest)
              .response(asJson[ConfigUpdateResponse])

            for
              response <- backend.send(request)
              _ = response.code shouldBe StatusCode.Ok
              updateResponse = response.body match
                case Right(value) => value
                case Left(error) =>
                  throw new RuntimeException(
                    s"Failed to parse response: $error"
                  )
              _ = updateResponse.success shouldBe true
              _ = updateResponse.message should include("restart required")
            yield ()
          }
        }
      }
    }
  }

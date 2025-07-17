package config

import analytics.repository.InMemoryStatisticsRepository
import api.controllers.{
  AnalyticsController,
  ConfigurationController,
  TypingTestController
}
import api.models.ApiModels.given
import api.server.ApiServer
import api.services.{AnalyticsService, TypingTestService}
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

import java.io.File

class ConfigurationIntegrationSpec
    extends AsyncWordSpec
    with AsyncIOSpec
    with Matchers
    with BeforeAndAfterEach:

  private val testConfigPath = "test-keyla-config.json"
  private val testPort = 9999
  private val testHost = "localhost"
  private val baseUri = Uri
    .parse(s"http://$testHost:$testPort")
    .getOrElse(throw new RuntimeException("Invalid URI"))

  override def beforeEach(): Unit =
    super.beforeEach()
    // Clean up any existing test config file
    val configFile = new File(testConfigPath)
    if configFile.exists() then configFile.delete()

  override def afterEach(): Unit =
    super.afterEach()
    // Clean up test config file
    val configFile = new File(testConfigPath)
    if configFile.exists() then configFile.delete()

  private def createTestServer(): IO[ApiServer] =
    for
      // Create test configuration
      testConfig <- IO.pure(
        AppConfig(
          database = DatabaseConfig(
            mongoUri = "mongodb://test:27017",
            databaseName = "test_db",
            profilesCollection = "test_profiles",
            testsCollection = "test_tests",
            useMongoDb = false
          ),
          server = ServerConfig(
            host = testHost,
            port = testPort,
            enableCors = true
          ),
          dictionary = DictionaryConfig(
            basePath = "src/test/resources/dictionaries",
            fileExtension = ".txt",
            autoCreateDirectories = true
          )
        )
      )

      // Create repositories
      profileRepository <- IO.pure(InMemoryProfileRepository())
      dictionaryRepository <- IO.pure(
        FileDictionaryRepository(
          testConfig.dictionary.basePath,
          testConfig.dictionary.fileExtension
        )
      )
      typingTestRepository <- IO.pure(InMemoryTypingTestRepository())
      analyticsRepository <- IO.pure(InMemoryStatisticsRepository())

      // Create configuration service
      configService <- ConfigurationService.create(
        testConfig,
        profileRepository,
        typingTestRepository,
        dictionaryRepository
      )

      // Create controllers and server
      typingTestService <- IO.pure(
        TypingTestService(
          profileRepository,
          dictionaryRepository,
          typingTestRepository
        )
      )

      analyticsService <- IO.pure(
        AnalyticsService(analyticsRepository)
      )
      configController <- IO.pure(ConfigurationController(configService))
      typingTestController <- IO.pure(TypingTestController(typingTestService))
      analyticsController <- IO.pure(AnalyticsController(analyticsService))
      server <- IO.pure(
        ApiServer(
          configController,
          typingTestController,
          analyticsController
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
              .get(baseUri.addPath("api", "config"))
              .response(asJson[ConfigListResponse])

            for
              response <- backend.send(request)
              _ = response.code shouldBe StatusCode.Ok
              configList = response.body.getOrElse(
                throw new RuntimeException("Failed to parse response")
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
              .get(baseUri.addPath("api", "config", "database", "mongoUri"))
              .response(asJson[ConfigEntry])

            for
              response <- backend.send(request)
              _ = response.code shouldBe StatusCode.Ok
              configEntry = response.body.getOrElse(
                throw new RuntimeException("Failed to parse response")
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
              .get(baseUri.addPath("api", "config", "nonexistent", "key"))

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

            val updateRequest = ConfigUpdateRequest(
              key = ConfigKey("database", "mongoUri"),
              value = "mongodb://updated:27017"
            )

            val request = basicRequest
              .put(baseUri.addPath("api", "config"))
              .body(updateRequest)
              .response(asJson[ConfigUpdateResponse])

            for
              response <- backend.send(request)
              _ = response.code shouldBe StatusCode.Ok
              updateResponse = response.body.getOrElse(
                throw new RuntimeException("Failed to parse response")
              )
              _ = updateResponse.success shouldBe true
              _ = updateResponse.updatedConfig.database.mongoUri shouldBe "mongodb://updated:27017"
              _ = updateResponse.message should include("reinitialized")
            yield ()
          }
        }
      }
    }

    "update a non-repository affecting configuration entry" in {
      createTestServer().flatMap { server =>
        server.resource(testPort, testHost).use { _ =>
          BlazeClientBuilder[IO].resource.use { client =>
            val backend = Http4sBackend.usingClient(client)

            val updateRequest = ConfigUpdateRequest(
              key = ConfigKey("server", "enableCors"),
              value = "false"
            )

            val request = basicRequest
              .put(baseUri.addPath("api", "config"))
              .body(updateRequest)
              .response(asJson[ConfigUpdateResponse])

            for
              response <- backend.send(request)
              _ = response.code shouldBe StatusCode.Ok
              updateResponse = response.body.getOrElse(
                throw new RuntimeException("Failed to parse response")
              )
              _ = updateResponse.success shouldBe true
              _ = updateResponse.updatedConfig.server.enableCors shouldBe false
              _ = updateResponse.message should not include ("reinitialized")
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

            val updateRequest = ConfigUpdateRequest(
              key = ConfigKey("server", "port"),
              value = "invalid_port"
            )

            val request = basicRequest
              .put(baseUri.addPath("api", "config"))
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
              .get(baseUri.addPath("api", "config", "current"))
              .response(asJson[AppConfig])

            for
              response <- backend.send(request)
              _ = response.code shouldBe StatusCode.Ok
              config = response.body.getOrElse(
                throw new RuntimeException("Failed to parse response")
              )
              _ = config.database.mongoUri shouldBe "mongodb://test:27017"
              _ = config.server.port shouldBe testPort
              _ = config.dictionary.basePath shouldBe "src/test/resources/dictionaries"
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

            // First update something
            val updateRequest = ConfigUpdateRequest(
              key = ConfigKey("database", "mongoUri"),
              value = "mongodb://modified:27017"
            )

            val updateReq = basicRequest
              .put(baseUri.addPath("api", "config"))
              .body(updateRequest)
              .response(asJson[ConfigUpdateResponse])

            // Then reset to defaults
            val resetRequest = basicRequest
              .post(baseUri.addPath("api", "config", "reset"))
              .response(asJson[AppConfig])

            for
              _ <- backend.send(updateReq)
              resetResponse <- backend.send(resetRequest)
              _ = resetResponse.code shouldBe StatusCode.Ok
              config = resetResponse.body.getOrElse(
                throw new RuntimeException("Failed to parse response")
              )
              _ = config.database.mongoUri shouldBe "mongodb://localhost:27017" // default value
              _ = config.server.port shouldBe 8080 // default value
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

            // 1. List all configuration entries (like git config --list)
            val listRequest = basicRequest
              .get(baseUri.addPath("api", "config"))
              .response(asJson[ConfigListResponse])

            // 2. Get a specific entry (like git config user.name)
            val getRequest = basicRequest
              .get(baseUri.addPath("api", "config", "database", "mongoUri"))
              .response(asJson[ConfigEntry])

            // 3. Set a value (like git config user.name "John Doe") - using relative path that can be created
            val customPath = "src/test/resources/custom-dictionaries"
            val setRequest = basicRequest
              .put(baseUri.addPath("api", "config"))
              .body(
                ConfigUpdateRequest(
                  key = ConfigKey("dictionary", "basePath"),
                  value = customPath
                )
              )
              .response(asJson[ConfigUpdateResponse])

            // 4. Verify the change
            val verifyRequest = basicRequest
              .get(baseUri.addPath("api", "config", "dictionary", "basePath"))
              .response(asJson[ConfigEntry])

            for
              // List all configs
              listResponse <- backend.send(listRequest)
              _ = listResponse.code shouldBe StatusCode.Ok
              configList = listResponse.body.getOrElse(
                throw new RuntimeException("Failed to parse list response")
              )
              _ = configList.entries.length should be > 5

              // Get specific config
              getResponse <- backend.send(getRequest)
              _ = getResponse.code shouldBe StatusCode.Ok
              originalEntry = getResponse.body.getOrElse(
                throw new RuntimeException("Failed to parse get response")
              )

              // Set new value
              setResponse <- backend.send(setRequest)
              _ = setResponse.code shouldBe StatusCode.Ok
              setResult = setResponse.body.getOrElse(
                throw new RuntimeException("Failed to parse set response")
              )
              _ = setResult.success shouldBe true
              _ = setResult.message should include("reinitialized")

              // Verify change
              verifyResponse <- backend.send(verifyRequest)
              _ = verifyResponse.code shouldBe StatusCode.Ok
              updatedEntry = verifyResponse.body.getOrElse(
                throw new RuntimeException("Failed to parse verify response")
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

            // Switch from in-memory to MongoDB
            val enableMongoRequest = basicRequest
              .put(baseUri.addPath("api", "config"))
              .body(
                ConfigUpdateRequest(
                  key = ConfigKey("database", "useMongoDb"),
                  value = "true"
                )
              )
              .response(asJson[ConfigUpdateResponse])

            // Change MongoDB URI
            val changeUriRequest = basicRequest
              .put(baseUri.addPath("api", "config"))
              .body(
                ConfigUpdateRequest(
                  key = ConfigKey("database", "mongoUri"),
                  value = "mongodb://newhost:27017"
                )
              )
              .response(asJson[ConfigUpdateResponse])

            // Switch back to in-memory
            val disableMongoRequest = basicRequest
              .put(baseUri.addPath("api", "config"))
              .body(
                ConfigUpdateRequest(
                  key = ConfigKey("database", "useMongoDb"),
                  value = "false"
                )
              )
              .response(asJson[ConfigUpdateResponse])

            for
              // Enable MongoDB - should reinitialize repositories
              enableResponse <- backend.send(enableMongoRequest)
              _ = enableResponse.code shouldBe StatusCode.Ok
              enableResult = enableResponse.body.getOrElse(
                throw new RuntimeException("Failed to parse response")
              )
              _ = enableResult.message should include("reinitialized")
              _ = enableResult.updatedConfig.database.useMongoDb shouldBe true

              // Change URI - should reinitialize repositories
              uriResponse <- backend.send(changeUriRequest)
              _ = uriResponse.code shouldBe StatusCode.Ok
              uriResult = uriResponse.body.getOrElse(
                throw new RuntimeException("Failed to parse response")
              )
              _ = uriResult.message should include("reinitialized")

              // Disable MongoDB - should reinitialize repositories
              disableResponse <- backend.send(disableMongoRequest)
              _ = disableResponse.code shouldBe StatusCode.Ok
              disableResult = disableResponse.body.getOrElse(
                throw new RuntimeException("Failed to parse response")
              )
              _ = disableResult.message should include("reinitialized")
              _ = disableResult.updatedConfig.database.useMongoDb shouldBe false
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

            val updateRequest = ConfigUpdateRequest(
              key = ConfigKey("server", "port"),
              value = "9090"
            )

            val request = basicRequest
              .put(baseUri.addPath("api", "config"))
              .body(updateRequest)
              .response(asJson[ConfigUpdateResponse])

            for
              response <- backend.send(request)
              _ = response.code shouldBe StatusCode.Ok
              updateResponse = response.body.getOrElse(
                throw new RuntimeException("Failed to parse response")
              )
              _ = updateResponse.success shouldBe true
              _ = updateResponse.message should include("restart required")
              _ = updateResponse.updatedConfig.server.port shouldBe 9090
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

            val updateRequest = ConfigUpdateRequest(
              key = ConfigKey("server", "host"),
              value = "0.0.0.0"
            )

            val request = basicRequest
              .put(baseUri.addPath("api", "config"))
              .body(updateRequest)
              .response(asJson[ConfigUpdateResponse])

            for
              response <- backend.send(request)
              _ = response.code shouldBe StatusCode.Ok
              updateResponse = response.body.getOrElse(
                throw new RuntimeException("Failed to parse response")
              )
              _ = updateResponse.success shouldBe true
              _ = updateResponse.message should include("restart required")
              _ = updateResponse.updatedConfig.server.host shouldBe "0.0.0.0"
            yield ()
          }
        }
      }
    }
  }

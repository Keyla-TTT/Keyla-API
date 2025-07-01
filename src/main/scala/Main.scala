import api.controllers.{ConfigurationController, TypingTestController}
import api.server.ApiServer
import api.services.TypingTestService
import cats.effect.{ExitCode, IO, IOApp}
import config.{AppConfig, ConfigurationService}

object Main extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =
    for
      config <- AppConfig.loadOrCreateDefault()

      profileRepository = AppConfig.createProfileRepository(config)
      dictionaryRepository = AppConfig.createDictionaryRepository(config)
      typingTestRepository = AppConfig.createTypingTestRepository(config)

      configService <- ConfigurationService.create(
        config,
        profileRepository,
        typingTestRepository,
        dictionaryRepository
      )

      service = TypingTestService(
        profileRepository,
        dictionaryRepository,
        typingTestRepository
      )
      configController = ConfigurationController(configService)
      controller = TypingTestController(service, configController)
      server = ApiServer(controller)

      _ <- IO.println(
        s"Starting server on ${config.server.host}:${config.server.port}"
      )
      _ <- IO.println(s"Using configuration file: ${AppConfig.getConfigPath}")
      _ <- IO.println(s"MongoDB enabled: ${config.database.useMongoDb}")
      _ <- IO.println(s"Dictionary path: ${config.dictionary.basePath}")

      exitCode <- server.serveOn(config.server.port, config.server.host)
    yield exitCode

import api.controllers.{
  AnalyticsController,
  ConfigurationController,
  TypingTestController
}
import api.server.ApiServer
import api.services.{AnalyticsService, StatisticsService, TypingTestService}
import analytics.calculator.AnalyticsCalculatorImpl
import cats.effect.{ExitCode, IO, IOApp}
import config.{AppConfig, ConfigurationService}

object Main extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =
    for
      config <- AppConfig.loadOrCreateDefault()

      profileRepository = AppConfig.createProfileRepository(config)
      dictionaryRepository = AppConfig.createDictionaryRepository(config)
      typingTestRepository = AppConfig.createTypingTestRepository(config)
      statisticsRepository = AppConfig.createStatisticsRepository(config)

      configService <- ConfigurationService.create(
        config,
        profileRepository,
        typingTestRepository,
        dictionaryRepository,
        statisticsRepository
      )

      service = TypingTestService(
        profileRepository,
        dictionaryRepository,
        typingTestRepository
      )
      statisticsService = StatisticsService(statisticsRepository)
      analyticsCalculator = AnalyticsCalculatorImpl()
      analyticsService = AnalyticsService(
        statisticsRepository,
        analyticsCalculator
      )

      configController = ConfigurationController(configService)
      typingTestController = TypingTestController(service)
      analyticsController = AnalyticsController(
        statisticsService,
        analyticsService
      )
      server = ApiServer(
        configController,
        typingTestController,
        analyticsController,
        config
      )

      _ <- IO.println(
        s"Starting server on ${config.server.host}:${config.server.port}"
      )
      _ <- IO.println(s"Using configuration file: ${AppConfig.getConfigPath}")
      _ <- IO.println(s"MongoDB enabled: ${config.database.useMongoDb}")
      _ <- IO.println(s"Dictionary path: ${config.dictionary.basePath}")

      exitCode <- server.serveOn(config.server.port, config.server.host)
    yield exitCode

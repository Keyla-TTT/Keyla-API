import api.controllers.analytics.AnalyticsController
import api.controllers.config.ConfigurationController
import api.controllers.stats.StatsController
import api.controllers.typingtest.TypingTestController
import api.controllers.users.UsersController
import api.server.ApiServer
import api.services.{
  AnalyticsService,
  ConfigurationService,
  ProfileService,
  StatisticsService,
  TypingTestService
}
import analytics.calculator.AnalyticsCalculatorImpl
import cats.effect.{ExitCode, IO, IOApp}
import config.{AppConfig, ConfigUtils}

object Main extends IOApp:

  override def run(args: List[String]): IO[ExitCode] =
    for
      config <- ConfigUtils.loadOrCreateDefault()

      profileRepository = ConfigUtils.createProfileRepository(config)
      dictionaryRepository = ConfigUtils.createDictionaryRepository(config)
      typingTestRepository = ConfigUtils.createTypingTestRepository(config)
      statisticsRepository = ConfigUtils.createStatisticsRepository(config)

      configService <- ConfigurationService.create(
        config,
        profileRepository,
        typingTestRepository,
        dictionaryRepository,
        statisticsRepository
      )

      typingTestService = TypingTestService(
        profileRepository,
        dictionaryRepository,
        typingTestRepository,
        statisticsRepository
      )
      profileService = ProfileService(profileRepository)
      statisticsService = StatisticsService(statisticsRepository)
      analyticsCalculator = AnalyticsCalculatorImpl()
      analyticsService = AnalyticsService(
        statisticsService,
        analyticsCalculator
      )

      configController = ConfigurationController(configService)
      usersController = UsersController(profileService)
      typingTestController = TypingTestController(typingTestService)
      statsController = StatsController(statisticsService)
      analyticsController = AnalyticsController(analyticsService)
      server = ApiServer(
        usersController,
        typingTestController,
        statsController,
        analyticsController,
        configController,
        config
      )

      _ <- IO.println(
        s"Starting server on ${config.server.host}:${config.server.port}"
      )
      _ <- IO.println(s"Using configuration file: ${ConfigUtils.getConfigPath}")
      _ <- IO.println(s"MongoDB enabled: ${config.database.useMongoDb}")
      _ <- IO.println(s"Dictionary path: ${config.dictionary.basePath}")
      _ <- IO.println(
        s"Thread pool: coreSize=${config.server.threadPool.coreSize}, maxSize=${config.server.threadPool.maxSize}"
      )

      exitCode <- server.resource(config.server.port, config.server.host).use {
        runningServer =>
          for
            _ <- IO.println(
              s"Server started successfully on ${runningServer.address}"
            )
            _ <- IO.println("Press Ctrl+C to stop the server...")
            _ <- IO.never
          yield ExitCode.Success
      }
    yield exitCode

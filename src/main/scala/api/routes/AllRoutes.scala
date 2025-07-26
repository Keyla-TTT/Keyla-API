package api.routes

import api.controllers.analytics.AnalyticsController
import api.controllers.config.ConfigurationController
import api.controllers.stats.StatsController
import api.controllers.typingtest.TypingTestController
import api.controllers.users.UsersController
import api.endpoints.AllEndpoints
import api.models.AppError
import api.models.common.ErrorResponse
import cats.effect.IO
import sttp.tapir.server.ServerEndpoint

object AllRoutes:

  private def handleControllerResult[A](
      result: IO[Either[AppError, A]]
  ): IO[Either[ErrorResponse, A]] =
    result.map(_.left.map(_.toErrorResponse))

  def createRoutes(
      usersController: UsersController,
      typingTestController: TypingTestController,
      statsController: StatsController,
      analyticsController: AnalyticsController,
      configController: ConfigurationController
  ): List[ServerEndpoint[Any, IO]] =
    List(
      AllEndpoints.createProfile.serverLogic(request =>
        handleControllerResult(usersController.createProfile(request))
      ),
      AllEndpoints.getAllProfiles.serverLogic(_ =>
        handleControllerResult(usersController.getAllProfiles())
      ),
      AllEndpoints.getProfileById.serverLogic(profileId =>
        handleControllerResult(usersController.getProfileById(profileId))
      ),
      AllEndpoints.updateProfile.serverLogic { case (profileId, request) =>
        handleControllerResult(
          usersController.updateProfile(profileId, request)
        )
      },
      AllEndpoints.deleteProfile.serverLogic(profileId =>
        handleControllerResult(usersController.deleteProfile(profileId))
      ),
      AllEndpoints.requestTest.serverLogic(request =>
        handleControllerResult(typingTestController.requestTest(request))
      ),
      AllEndpoints.getTestById.serverLogic(testId =>
        handleControllerResult(typingTestController.getTestById(testId))
      ),
      AllEndpoints.getTestsByProfileId.serverLogic(profileId =>
        handleControllerResult(
          typingTestController.getTestsByProfileId(profileId)
        )
      ),
      AllEndpoints.getLastTest.serverLogic(profileId =>
        handleControllerResult(typingTestController.getLastTest(profileId))
      ),
      AllEndpoints.submitTestResults.serverLogic { case (testId, results) =>
        handleControllerResult(
          typingTestController.submitTestResults(testId, results)
        )
      },
      AllEndpoints.getAllDictionaries.serverLogic(_ =>
        handleControllerResult(typingTestController.getAllDictionaries())
      ),
      AllEndpoints.getAllModifiers.serverLogic(_ =>
        handleControllerResult(typingTestController.getAllModifiers())
      ),
      AllEndpoints.getAllMergers.serverLogic(_ =>
        handleControllerResult(typingTestController.getAllMergers())
      ),
      AllEndpoints.saveStatistics.serverLogic(request =>
        handleControllerResult(statsController.saveStatistics(request))
      ),
      AllEndpoints.getAllProfileStatistics.serverLogic(profileId =>
        handleControllerResult(
          statsController.getAllProfileStatistics(profileId)
        )
      ),
      AllEndpoints.getUserAnalytics.serverLogic(userId =>
        handleControllerResult(analyticsController.getUserAnalytics(userId))
      ),
      AllEndpoints.getAllConfigEntries.serverLogic(_ =>
        handleControllerResult(configController.getAllConfigEntries())
      ),
      AllEndpoints.getConfigEntry.serverLogic(key =>
        handleControllerResult(configController.getConfigEntry(key))
      ),
      AllEndpoints.updateConfigEntry.serverLogic(request =>
        handleControllerResult(
          configController.updateConfigEntrySimple(request)
        )
      ),
      AllEndpoints.getCurrentConfig.serverLogic(_ =>
        handleControllerResult(configController.getCurrentConfig)
      ),
      AllEndpoints.reloadConfig.serverLogic(_ =>
        handleControllerResult(configController.reloadConfig())
      ),
      AllEndpoints.resetConfigToDefaults.serverLogic(_ =>
        handleControllerResult(configController.resetToDefaults())
      )
    )

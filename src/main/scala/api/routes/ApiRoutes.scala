package api.routes

import api.controllers.TypingTestController
import api.endpoints.ApiEndpoints
import api.models.{AppError, ErrorResponse}
import cats.effect.IO
import sttp.tapir.server.ServerEndpoint

class ApiRoutes(controller: TypingTestController):

  private def handleControllerResult[A](
      result: IO[Either[AppError, A]]
  ): IO[Either[ErrorResponse, A]] =
    result.map(_.left.map(_.toErrorResponse))

  val routes: List[ServerEndpoint[Any, IO]] = List(
    ApiEndpoints.createProfile.serverLogic { request =>
      handleControllerResult(controller.createProfile(request))
    },
    ApiEndpoints.getAllProfiles.serverLogic { _ =>
      handleControllerResult(controller.getAllProfiles())
    },
    ApiEndpoints.requestTest.serverLogic { request =>
      handleControllerResult(controller.requestTest(request))
    },
    ApiEndpoints.getTestById.serverLogic { testId =>
      handleControllerResult(controller.getTestById(testId))
    },
    ApiEndpoints.getTestsByProfileId.serverLogic { profileId =>
      handleControllerResult(controller.getTestsByProfileId(profileId))
    },
    ApiEndpoints.getTestsByLanguage.serverLogic { language =>
      handleControllerResult(controller.getTestsByLanguage(language))
    },
    ApiEndpoints.getLastTest.serverLogic { profileId =>
      handleControllerResult(controller.getLastTest(profileId))
    },
    ApiEndpoints.submitTestResults.serverLogic { case (testId, results) =>
      handleControllerResult(controller.submitTestResults(testId, results))
    },
    ApiEndpoints.getAllDictionaries.serverLogic { _ =>
      handleControllerResult(controller.getAllDictionaries())
    },
    ApiEndpoints.getLanguages.serverLogic { _ =>
      handleControllerResult(controller.getLanguages())
    },
    ApiEndpoints.getDictionariesByLanguage.serverLogic { language =>
      handleControllerResult(controller.getDictionariesByLanguage(language))
    },
    // Configuration routes
    ApiEndpoints.getAllConfigEntries.serverLogic { _ =>
      handleControllerResult(controller.getAllConfigEntries())
    },
    ApiEndpoints.getConfigEntry.serverLogic { case (section, key) =>
      handleControllerResult(controller.getConfigEntry(section, key))
    },
    ApiEndpoints.updateConfigEntry.serverLogic { request =>
      handleControllerResult(controller.updateConfigEntry(request))
    },
    ApiEndpoints.getCurrentConfig.serverLogic { _ =>
      handleControllerResult(controller.getCurrentConfig())
    },
    ApiEndpoints.reloadConfig.serverLogic { _ =>
      handleControllerResult(controller.reloadConfig())
    },
    ApiEndpoints.resetConfigToDefaults.serverLogic { _ =>
      handleControllerResult(controller.resetConfigToDefaults())
    }
  )

package api.controllers.stats

import api.models.AppError
import api.models.AppResult
import api.models.stats.*
import api.services.StatisticsService
import cats.effect.IO
import config.*

class StatsController(
    statisticsService: StatisticsService
):

  def saveStatistics(
      request: SaveStatisticsRequest
  ): IO[Either[AppError, StatisticsResponse]] =
    handleServiceResult(statisticsService.saveStatistics(request))

  def getAllProfileStatistics(
      profileId: String
  ): IO[Either[AppError, ProfileStatisticsListResponse]] =
    handleServiceResult(statisticsService.getAllProfileStatistics(profileId))

  def getStatistics(testId: String): IO[Either[AppError, StatisticsResponse]] =
    handleServiceResult(statisticsService.getStatistics(testId))

  def deleteAllUserStatistics(userId: String): IO[Either[AppError, Boolean]] =
    handleServiceResult(statisticsService.deleteAllUserStatistics(userId))

  def listUserStatistics(
      userId: String
  ): IO[Either[AppError, List[StatisticsResponse]]] =
    handleServiceResult(statisticsService.listUserStatistics(userId))

  def cleanRepository(): IO[Either[AppError, Boolean]] =
    handleServiceResult(statisticsService.cleanRepository())

  private def handleServiceResult[A](
      serviceCall: AppResult[A]
  ): IO[Either[AppError, A]] =
    serviceCall.value

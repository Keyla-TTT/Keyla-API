package api.controllers

import api.models.*
import api.services.{AnalyticsService, StatisticsService}
import cats.effect.IO

class AnalyticsController(
    statisticsService: StatisticsService,
    analyticsService: AnalyticsService
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

  def getUserAnalytics(
      userId: String
  ): IO[Either[AppError, AnalyticsResponse]] =
    handleServiceResult(analyticsService.getUserAnalytics(userId))

  private def handleServiceResult[A](
      serviceCall: AppResult[A]
  ): IO[Either[AppError, A]] =
    serviceCall.value

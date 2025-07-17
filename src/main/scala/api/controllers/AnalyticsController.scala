package api.controllers

import api.models.*
import api.services.{AnalyticsService, TypingTestService}
import cats.effect.IO
import config.*

class AnalyticsController(
    analyticsService: AnalyticsService
):

  def saveStatistics(
      request: SaveStatisticsRequest
  ): IO[Either[AppError, StatisticsResponse]] =
    handleServiceResult(analyticsService.saveStatistics(request))

  def getAllProfileStatistics(
      profileId: String
  ): IO[Either[AppError, ProfileStatisticsListResponse]] =
    handleServiceResult(analyticsService.getAllProfileStatistics(profileId))

  private def handleServiceResult[A](
      serviceCall: AppResult[A]
  ): IO[Either[AppError, A]] =
    serviceCall.value

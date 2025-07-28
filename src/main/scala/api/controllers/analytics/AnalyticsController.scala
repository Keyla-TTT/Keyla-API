package api.controllers.analytics

import api.models.AppError
import api.models.AppResult
import api.models.analytics.*
import api.services.AnalyticsService
import cats.effect.IO
import config.*

class AnalyticsController(
    analyticsService: AnalyticsService
):

  def getUserAnalytics(
      userId: String
  ): IO[Either[AppError, AnalyticsResponse]] =
    handleServiceResult(analyticsService.getUserAnalytics(userId))

  private def handleServiceResult[A](
      serviceCall: AppResult[A]
  ): IO[Either[AppError, A]] =
    serviceCall.value

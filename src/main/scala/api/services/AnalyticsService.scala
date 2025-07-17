package api.services

import analytics.model.{Statistics, UserStatistics, UserStatisticsBuilder}
import analytics.repository.StatisticsRepository
import analytics.service.StatisticsServiceImpl
import api.models.*
import api.models.AppError.*
import cats.effect.IO

trait AnalyticsService:

  def saveStatistics(
      request: SaveStatisticsRequest
  ): AppResult[StatisticsResponse]

  def getAllProfileStatistics(
      profileId: String
  ): AppResult[ProfileStatisticsListResponse]

object AnalyticsService:

  def apply(repository: StatisticsRepository): AnalyticsService =
    AnalyticsServiceImpl(repository)

case class AnalyticsServiceImpl(repository: StatisticsRepository)
    extends AnalyticsService:

  def saveStatistics(
      request: SaveStatisticsRequest
  ): AppResult[StatisticsResponse] =
    for
      newStatistics <- AppResult.pure(
        UserStatistics(
          testId = None,
          userId = request.profileId,
          wpm = request.wpm,
          accuracy = request.accuracy,
          errors = request.errors,
          timestamp = System.currentTimeMillis()
        )
      )
      savedStatistics <- AppResult.attemptBlocking(
        IO.blocking(repository.save(newStatistics))
      )(error => StatisticsSavingError(error.getMessage))
      response <- AppResult.pure(
        ApiModels.statisticsToResponse(savedStatistics)
      )
    yield response

    def getAllProfileStatistics(
        profileId: String
    ): AppResult[ProfileStatisticsListResponse] =
      AppResult
        .attemptBlocking(
          IO.blocking(repository.list(profileId))
        )(error => DatabaseError("statistics lookup", error.getMessage))
        .map(ApiModels.profileStatisticsListToResponse)

package api.services

import analytics.calculator.AnalyticsCalculator
import analytics.model.UserStatistics
import analytics.repository.StatisticsRepository
import api.models.*
import api.models.AppError.*
import cats.effect.IO

trait AnalyticsService:

  def getUserAnalytics(userId: String): AppResult[AnalyticsResponse]

object AnalyticsService:

  def apply(
      repository: StatisticsRepository,
      calculator: AnalyticsCalculator
  ): AnalyticsService =
    AnalyticsServiceImpl(repository, calculator)

case class AnalyticsServiceImpl(
    repository: StatisticsRepository,
    calculator: AnalyticsCalculator
) extends AnalyticsService:

  def getUserAnalytics(userId: String): AppResult[AnalyticsResponse] =
    for
      statisticsList <- AppResult.attemptBlocking(
        IO.blocking(repository.list(userId))
      )(error => DatabaseError("statistics lookup", error.getMessage))
      userStatistics <- AppResult.pure(
        statisticsList.map(stat =>
          UserStatistics(
            testId = stat.testId,
            userId = stat.userId,
            wpm = stat.wpm,
            accuracy = stat.accuracy,
            errors = stat.errors,
            timestamp = stat.timestamp
          )
        )
      )
      analytics <- AppResult.pure(calculator.analyzeUser(userStatistics))
      response <- AppResult.pure(
        AnalyticsResponse(
          userId = analytics.userId,
          totalTests = analytics.totalTests,
          averageWpm = analytics.averageWpm,
          averageAccuracy = analytics.averageAccuracy,
          bestWpm = analytics.bestWpm,
          worstWpm = analytics.worstWpm,
          bestAccuracy = analytics.bestAccuracy,
          worstAccuracy = analytics.worstAccuracy,
          wpmImprovement = analytics.wpmImprovement,
          accuracyImprovement = analytics.accuracyImprovement,
          totalErrors = analytics.totalErrors,
          averageErrorsPerTest = analytics.averageErrorsPerTest
        )
      )
    yield response

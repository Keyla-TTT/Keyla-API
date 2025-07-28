package api.services

import analytics.calculator.AnalyticsCalculator
import analytics.model.TestStatistics
import api.models.AppError.*
import api.models.AppResult
import api.models.analytics.*
import cats.effect.IO

/** API-level service for generating user analytics and performance insights.
  *
  * This service provides the main interface for analytics operations in the
  * Keyla typing test application. It processes user statistics to generate
  * comprehensive performance reports and insights.
  *
  * The service orchestrates interactions between the statistics service and
  * analytics calculator to produce meaningful user performance analysis. All
  * operations return `AppResult[T]` which provides consistent error handling
  * and composability.
  *
  * =Key Features=
  *
  *   - '''Performance Analysis''': Calculate averages, trends, and improvements
  *   - '''User Insights''': Generate comprehensive user performance reports
  *   - '''Statistical Aggregation''': Process multiple test results
  *   - '''Trend Tracking''': Monitor performance improvements over time
  *   - '''Error Analysis''': Analyze error patterns and frequencies
  *   - '''Data Transformation''': Convert between domain and API models
  *
  * =Service Architecture=
  *
  * The service follows a layered architecture:
  * {{{
  * REST API Controller
  *        ↓
  * AnalyticsService (this layer)
  *        ↓
  * StatisticsService + AnalyticsCalculator
  *        ↓
  * Data Storage + Calculation Logic
  * }}}
  *
  * =Error Handling=
  *
  * All service methods return `AppResult[T]` which encapsulates either success
  * or typed errors:
  *   - '''Analytics Errors''': AnalyticsCalculationFailed
  *   - '''Statistics Errors''': StatisticsRetrievalFailed
  *   - '''Database Errors''': DatabaseError
  *
  * @example
  *   {{{
  * // Create an analytics service
  * val service = AnalyticsService(statisticsService, analyticsCalculator)
  *
  * // Get user analytics
  * val analytics = service.getUserAnalytics("user-123")
  * analytics.value.map {
  *   case Right(response) =>
  *     println(s"User average WPM: ${response.averageWpm}")
  *     println(s"Improvement: ${response.wpmImprovement}%")
  *   case Left(error) => println(s"Error: ${error.message}")
  * }
  *   }}}
  */
trait AnalyticsService:

  /** Generates comprehensive analytics for a specific user.
    *
    * This operation retrieves all statistics for the user, processes them
    * through the analytics calculator, and returns a comprehensive report with
    * performance metrics, trends, and insights.
    *
    * @param userId
    *   The unique identifier of the user
    * @return
    *   AppResult containing the analytics response or an error
    *
    * @example
    *   {{{
    * service.getUserAnalytics("user-456").value.map {
    *   case Right(analytics) =>
    *     println(s"Total tests: ${analytics.totalTests}")
    *     println(s"Average WPM: ${analytics.averageWpm}")
    *     println(s"Best WPM: ${analytics.bestWpm}")
    *     println(s"WPM improvement: ${analytics.wpmImprovement}%")
    *     println(s"Total errors: ${analytics.totalErrors}")
    *   case Left(error) => println(s"Error: ${error.message}")
    * }
    *   }}}
    */
  def getUserAnalytics(userId: String): AppResult[AnalyticsResponse]

object AnalyticsService:

  def apply(
      statisticsService: StatisticsService,
      calculator: AnalyticsCalculator
  ): AnalyticsService =
    AnalyticsServiceImpl(statisticsService, calculator)

case class AnalyticsServiceImpl(
    statisticsService: StatisticsService,
    calculator: AnalyticsCalculator
) extends AnalyticsService:

  def getUserAnalytics(userId: String): AppResult[AnalyticsResponse] =
    for
      statisticsList <- statisticsService.listUserStatistics(userId)
      userStatistics <- AppResult.pure(
        statisticsList.map(stat =>
          TestStatistics(
            testId = stat.testId,
            userId = stat.profileId,
            wpm = stat.wpm,
            accuracy = stat.accuracy,
            errors = stat.errors,
            timestamp = stat.timestamp
          )
        )
      )
      analytics <- AppResult.pure(
        calculator.analyzeUser(userStatistics, userId)
      )
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

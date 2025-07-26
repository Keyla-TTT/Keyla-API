package api.services

import analytics.model.{Statistics, TestStatistics}
import analytics.repository.StatisticsRepository
import api.models.AppError.*
import api.models.AppResult
import api.models.stats.*
import cats.effect.IO

/** API-level service for managing typing test statistics.
  *
  * This service provides the main interface for statistics operations in the
  * Keyla typing test application. It handles statistics storage, retrieval, and
  * management with proper error handling and business logic validation.
  *
  * The service orchestrates interactions with the statistics repository and
  * handles data transformation between domain models and API response models.
  * All operations return `AppResult[T]` which provides consistent error
  * handling and composability.
  *
  * =Key Features=
  *
  *   - '''Statistics Storage''': Save test results with performance metrics
  *   - '''Statistics Retrieval''': Get individual statistics or list by user
  *   - '''User Analytics''': Aggregate statistics for user performance analysis
  *   - '''Data Cleanup''': Remove old or unwanted statistics
  *   - '''Error Handling''': Comprehensive error handling with typed errors
  *   - '''Data Transformation''': Convert between domain and API models
  *
  * =Service Architecture=
  *
  * The service follows a layered architecture:
  * {{{
  * REST API Controller
  *        ↓
  * StatisticsService (this layer)
  *        ↓
  * StatisticsRepository
  *        ↓
  * Data Storage (MongoDB or In-Memory)
  * }}}
  *
  * =Error Handling=
  *
  * All service methods return `AppResult[T]` which encapsulates either success
  * or typed errors:
  *   - '''Statistics Errors''': StatisticsSavingFailed
  *   - '''Database Errors''': DatabaseError
  *   - '''Test Errors''': TestNotFound
  *
  * @example
  *   {{{
  * // Create a statistics service
  * val service = StatisticsService(statisticsRepository)
  *
  * // Save test statistics
  * val request = SaveStatisticsRequest(
  *   testId = "test-123",
  *   profileId = "user-456",
  *   wpm = 75.5,
  *   accuracy = 95.2,
  *   errors = List(10, 25, 40),
  *   timestamp = System.currentTimeMillis()
  * )
  * val result = service.saveStatistics(request)
  *
  * // Get user statistics
  * val userStats = service.getAllProfileStatistics("user-456")
  *   }}}
  */
trait StatisticsService:

  /** Saves typing test statistics to the repository.
    *
    * This operation validates the input data and stores the statistics for
    * future analysis and user performance tracking.
    *
    * @param request
    *   The statistics save request containing test results
    * @return
    *   AppResult containing the saved statistics response or an error
    *
    * @example
    *   {{{
    * val request = SaveStatisticsRequest(
    *   testId = "test-789",
    *   profileId = "user-123",
    *   wpm = 80.0,
    *   accuracy = 97.5,
    *   errors = List(5, 15),
    *   timestamp = System.currentTimeMillis()
    * )
    * service.saveStatistics(request).value.map {
    *   case Right(response) => println(s"Saved stats for test: ${response.testId}")
    *   case Left(error) => println(s"Failed: ${error.message}")
    * }
    *   }}}
    */
  def saveStatistics(
      request: SaveStatisticsRequest
  ): AppResult[StatisticsResponse]

  /** Retrieves all statistics for a specific user profile.
    *
    * This operation fetches all statistics from the repository for the
    * specified user and transforms them into API response format.
    *
    * @param profileId
    *   The unique identifier of the user profile
    * @return
    *   AppResult containing the list of statistics or an error
    *
    * @example
    *   {{{
    * service.getAllProfileStatistics("user-456").value.map {
    *   case Right(response) => println(s"Found ${response.statistics.length} tests")
    *   case Left(error) => println(s"Error: ${error.message}")
    * }
    *   }}}
    */
  def getAllProfileStatistics(
      profileId: String
  ): AppResult[ProfileStatisticsListResponse]

  /** Retrieves statistics for a specific test.
    *
    * @param testId
    *   The unique identifier of the test
    * @return
    *   AppResult containing the statistics response or an error
    */
  def getStatistics(testId: String): AppResult[StatisticsResponse]

  /** Deletes all statistics for a specific user.
    *
    * @param userId
    *   The unique identifier of the user
    * @return
    *   AppResult containing the deletion result or an error
    */
  def deleteAllUserStatistics(userId: String): AppResult[Boolean]

  /** Lists all statistics for a specific user.
    *
    * @param userId
    *   The unique identifier of the user
    * @return
    *   AppResult containing the list of statistics or an error
    */
  def listUserStatistics(userId: String): AppResult[List[StatisticsResponse]]

  /** Cleans up the statistics repository.
    *
    * @return
    *   AppResult containing the cleanup result or an error
    */
  def cleanRepository(): AppResult[Boolean]

object StatisticsService:

  def apply(repository: StatisticsRepository): StatisticsService =
    StatisticsServiceImpl(repository)

case class StatisticsServiceImpl(repository: StatisticsRepository)
    extends StatisticsService:

  def saveStatistics(
      request: SaveStatisticsRequest
  ): AppResult[StatisticsResponse] =
    for
      newStatistics <- AppResult.pure(
        TestStatistics(
          testId = request.testId,
          userId = request.profileId,
          wpm = request.wpm,
          accuracy = request.accuracy,
          errors = request.errors,
          timestamp = request.timestamp
        )
      )
      savedStatistics <- AppResult.attemptBlocking(
        IO.blocking(repository.save(newStatistics))
      )(error => StatisticsSavingFailed(error.getMessage))
      response: StatisticsResponse <- AppResult.pure(
        StatsModels.statisticsToResponse(savedStatistics)
      )
    yield response

  def getAllProfileStatistics(
      profileId: String
  ): AppResult[ProfileStatisticsListResponse] =
    AppResult
      .attemptBlocking(
        IO.blocking(repository.list(profileId))
      )(error => DatabaseError("profiles lookup", error.getMessage))
      .flatMap { statisticsList =>
        if statisticsList.isEmpty then
          AppResult.pure(ProfileStatisticsListResponse(profileId, List.empty))
        else
          val response = ProfileStatisticsListResponse(
            profileId,
            statisticsList.map(StatsModels.statisticsToResponse)
          )
          AppResult.pure(response)
      }

  def getStatistics(testId: String): AppResult[StatisticsResponse] =
    AppResult
      .fromOptionF(
        AppResult
          .attemptBlocking(
            IO.blocking(repository.get(testId))
          )(error => DatabaseError("statistics lookup", error.getMessage))
          .value
          .map(_.toOption.flatten),
        TestNotFound(testId)
      )
      .map(StatsModels.statisticsToResponse)

  def deleteAllUserStatistics(userId: String): AppResult[Boolean] =
    AppResult.attemptBlocking(
      IO.blocking(repository.deleteAll(userId))
    )(error => DatabaseError("delete user statistics", error.getMessage))

  def listUserStatistics(userId: String): AppResult[List[StatisticsResponse]] =
    AppResult
      .attemptBlocking(
        IO.blocking(repository.list(userId))
      )(error => DatabaseError("list user statistics", error.getMessage))
      .map(_.map(StatsModels.statisticsToResponse))

  def cleanRepository(): AppResult[Boolean] =
    AppResult.attemptBlocking(
      IO.blocking(repository.clean())
    )(error => DatabaseError("clean repository", error.getMessage))

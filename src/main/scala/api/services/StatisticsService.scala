package api.services

import analytics.model.{Statistics, UserStatistics}
import analytics.repository.StatisticsRepository
import api.models.*
import api.models.AppError.*
import cats.effect.IO

trait StatisticsService:

  def saveStatistics(
      request: SaveStatisticsRequest
  ): AppResult[StatisticsResponse]

  def getAllProfileStatistics(
      profileId: String
  ): AppResult[ProfileStatisticsListResponse]

  def getStatistics(testId: String): AppResult[StatisticsResponse]

  def deleteAllUserStatistics(userId: String): AppResult[Boolean]

  def listUserStatistics(userId: String): AppResult[List[StatisticsResponse]]

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
        UserStatistics(
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
        ApiModels.statisticsToResponse(savedStatistics)
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
            statisticsList.map(ApiModels.statisticsToResponse)
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
      .map(ApiModels.statisticsToResponse)

  def deleteAllUserStatistics(userId: String): AppResult[Boolean] =
    AppResult.attemptBlocking(
      IO.blocking(repository.deleteAll(userId))
    )(error => DatabaseError("delete user statistics", error.getMessage))

  def listUserStatistics(userId: String): AppResult[List[StatisticsResponse]] =
    AppResult
      .attemptBlocking(
        IO.blocking(repository.list(userId))
      )(error => DatabaseError("list user statistics", error.getMessage))
      .map(_.map(ApiModels.statisticsToResponse))

  def cleanRepository(): AppResult[Boolean] =
    AppResult.attemptBlocking(
      IO.blocking(repository.clean())
    )(error => DatabaseError("clean repository", error.getMessage))

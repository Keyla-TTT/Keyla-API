package api.controllers.typingtest

import api.models.AppError
import api.models.AppResult
import api.models.typingtest.*
import api.services.TypingTestService
import cats.effect.IO
import config.*

class TypingTestController(
    service: TypingTestService
):

  private def handleServiceResult[A](
      serviceCall: AppResult[A]
  ): IO[Either[AppError, A]] =
    serviceCall.value

  private def handleServiceResultWithTransform[A, B](
      serviceCall: AppResult[A]
  )(transform: A => B): IO[Either[AppError, B]] =
    serviceCall.value.map(_.map(transform))

  def requestTest(request: TestRequest): IO[Either[AppError, TestResponse]] =
    handleServiceResult(service.requestTest(request))

  def getTestById(testId: String): IO[Either[AppError, TestResponse]] =
    handleServiceResult(service.getTestById(testId))

  def getTestsByProfileId(
      profileId: String
  ): IO[Either[AppError, TestListResponse]] =
    handleServiceResultWithTransform(service.getTestsByProfileId(profileId))(
      TypingTestModels.testListToResponse
    )

  def getLastTest(profileId: String): IO[Either[AppError, LastTestResponse]] =
    handleServiceResult(service.getLastTest(profileId))

  def submitTestResults(
      testId: String,
      results: TestResultsRequest
  ): IO[Either[AppError, TestResponse]] =
    handleServiceResult(service.submitTestResults(testId, results))

  def getAllDictionaries(): IO[Either[AppError, DictionariesResponse]] =
    handleServiceResult(service.getAllDictionaries())

  def getAllModifiers(): IO[Either[AppError, ModifiersResponse]] =
    handleServiceResult(service.getAllModifiers())

  def getAllMergers(): IO[Either[AppError, MergersResponse]] =
    handleServiceResult(service.getAllMergers())

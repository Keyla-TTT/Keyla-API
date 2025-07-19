package api.controllers

import api.models.*
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

  def createProfile(
      request: CreateProfileRequest
  ): IO[Either[AppError, ProfileResponse]] =
    handleServiceResult(service.createProfile(request))

  def getAllProfiles(): IO[Either[AppError, ProfileListResponse]] =
    handleServiceResult(service.getAllProfiles())

  def requestTest(request: TestRequest): IO[Either[AppError, TestResponse]] =
    handleServiceResult(service.requestTest(request))

  def getTestById(testId: String): IO[Either[AppError, TestResponse]] =
    handleServiceResult(service.getTestById(testId))

  def getTestsByProfileId(
      profileId: String
  ): IO[Either[AppError, TestListResponse]] =
    handleServiceResultWithTransform(service.getTestsByProfileId(profileId))(
      ApiModels.testListToResponse
    )

  def getTestsByLanguage(
      language: String
  ): IO[Either[AppError, TestListResponse]] =
    handleServiceResultWithTransform(service.getTestsByLanguage(language))(
      ApiModels.testListToResponse
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

  def getLanguages(): IO[Either[AppError, LanguagesResponse]] =
    handleServiceResult(service.getLanguages())

  def getDictionariesByLanguage(
      language: String
  ): IO[Either[AppError, DictionariesResponse]] =
    handleServiceResult(service.getDictionariesByLanguage(language))

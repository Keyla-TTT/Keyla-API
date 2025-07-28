package api.services

import analytics.model.TestStatistics
import analytics.repository.StatisticsRepository
import api.models.AppError.*
import api.models.AppResult
import api.models.typingtest.*
import cats.effect.IO
import org.joda.time.DateTime
import typingTest.dictionary.loader.{
  DictionaryLoader,
  FileDictionaryLoader,
  JsonDictionaryLoader,
  MixedDictionaryLoader
}
import typingTest.dictionary.model.Dictionary
import typingTest.dictionary.repository.DictionaryRepository
import typingTest.tests.factory.TypingTestFactory
import typingTest.tests.model.{
  CompletedInfo,
  DefaultContext,
  ModifiersFacade,
  PersistedTypingTest,
  TypingTest
}
import typingTest.tests.repository.TypingTestRepository
import users_management.model.Profile
import users_management.repository.ProfileRepository
import typingTest.tests.factory.TypingTestFactory.copy

/** Core business logic service for managing typing tests and dictionaries.
  *
  * This service provides the main functionality of the Keyla typing test
  * application:
  *   - Typing test lifecycle management (creation, execution, completion)
  *   - Dictionary and language operations
  *   - Test result processing and storage
  *
  * The service orchestrates interactions between multiple repositories and
  * handles business logic validation, error handling, and data transformation.
  * All operations return `AppResult[T]` which provides consistent error
  * handling and composability.
  *
  * =Key Features=
  *
  *   - '''Test Creation''': Generate typing tests with configurable parameters
  *     and modifiers
  *   - '''Dictionary Integration''': Support for multiple languages and
  *     dictionary sources
  *   - '''Modifier System''': Apply text transformations (punctuation, numbers,
  *     etc.)
  *   - '''Result Tracking''': Store and retrieve test results with detailed
  *     metrics
  *   - '''State Management''': Handle test lifecycle (created → in-progress →
  *     completed)
  *   - '''Validation''': Comprehensive input validation and business rule
  *     enforcement
  *
  * =Service Architecture=
  *
  * The service follows a layered architecture:
  * {{{
  * REST API Controller
  *        ↓
  * TypingTestService (this layer)
  *        ↓
  * Repositories (ProfileRepository, TypingTestRepository, DictionaryRepository)
  *        ↓
  * Data Storage (MongoDB or In-Memory)
  * }}}
  *
  * =Error Handling=
  *
  * All service methods return `AppResult[T]` which encapsulates either success
  * or typed errors:
  *   - '''Profile Errors''': ProfileNotFound
  *   - '''Test Errors''': TestNotFound, TestAlreadyCompleted,
  *     TestCreationFailed
  *   - '''Dictionary Errors''': DictionaryNotFound, LanguageNotSupported
  *   - '''Validation Errors''': InvalidModifier, ValidationError
  *   - '''System Errors''': DatabaseError, FileSystemError
  *
  * @example
  *   {{{
  * // Create a typing test service
  * val service = TypingTestService(profileRepo, dictionaryRepo, testRepo)
  *
  * // Request a typing test
  * val testRequest = TestRequest(
  *   profileId = "profile-123",
  *   language = "english",
  *   dictionaryName = "common_words",
  *   wordCount = 50,
  *   modifiers = List("punctuation", "numbers"),
  *   timeLimit = Some(60000) // 60 seconds
  * )
  * val testResult = service.requestTest(testRequest)
  *
  * // Submit test results
  * val results = TestResultsRequest(
  *   accuracy = 95.5,
  *   rawAccuracy = 92.3,
  *   testTime = 45000,
  *   errorCount = 3,
  *   errorWordIndices = List(10, 25, 40)
  * )
  * val completedTest = service.submitTestResults("test-123", results)
  *   }}}
  */
trait TypingTestService:

  /** Creates a new typing test with the specified parameters.
    *
    * This operation:
    *   1. Validates the modifiers and profile existence
    *   2. Deletes any existing non-completed tests for the profile
    *   3. Loads the specified dictionary
    *   4. Generates a typing test with applied modifiers
    *   5. Limits the word count as requested
    *   6. Persists the test for future retrieval
    *
    * @param request
    *   The test creation request with parameters
    * @return
    *   AppResult containing the created test response or an error
    *
    * @example
    *   {{{
    * val request = TestRequest(
    *   profileId = "user-456",
    *   language = "english",
    *   dictionaryName = "programming_terms",
    *   wordCount = 100,
    *   modifiers = List("punctuation", "camelCase"),
    *   timeLimit = Some(120000) // 2 minutes
    * )
    * service.requestTest(request).value.map {
    *   case Right(response) => println(s"Test created: ${response.testId}")
    *   case Left(InvalidModifier(mod, available)) =>
    *     println(s"Invalid modifier '$mod', available: ${available.mkString(", ")}")
    *   case Left(error) => println(s"Error: ${error.message}")
    * }
    *   }}}
    */
  def requestTest(request: TestRequest): AppResult[TestResponse]

  /** Retrieves a completed typing test by its unique identifier. Only returns
    * completed tests - non-completed tests are not accessible via this method.
    *
    * @param testId
    *   The unique identifier of the test
    * @return
    *   AppResult containing the test response or an error
    *
    * @example
    *   {{{
    * service.getTestById("test-789").value.map {
    *   case Right(response) =>
    *     println(s"Test accuracy: ${response.accuracy.getOrElse("N/A")}%")
    *   case Left(TestNotFound(id)) => println(s"Test $id not found")
    *   case Left(error) => println(s"Error: ${error.message}")
    * }
    *   }}}
    */
  def getTestById(testId: String): AppResult[TestResponse]

  /** Retrieves all typing tests associated with a specific user profile.
    * Returns both completed and non-completed tests.
    *
    * @param profileId
    *   The unique identifier of the user profile
    * @return
    *   AppResult containing the list of tests or an error
    *
    * @example
    *   {{{
    * service.getTestsByProfileId("user-123").value.map {
    *   case Right(tests) =>
    *     val completed = tests.count(_.isCompleted)
    *     println(s"Found ${tests.length} tests, $completed completed")
    *   case Left(error) => println(s"Error: ${error.message}")
    * }
    *   }}}
    */
  def getTestsByProfileId(
      profileId: String
  ): AppResult[List[PersistedTypingTest]]

  /** Retrieves the most recent non-completed test for a user profile. Allows
    * users to resume interrupted typing tests.
    *
    * @param profileId
    *   The unique identifier of the user profile
    * @return
    *   AppResult containing the last test response or an error
    *
    * @example
    *   {{{
    * service.getLastTest("user-456").value.map {
    *   case Right(response) =>
    *     println(s"Resume test: ${response.testId} (${response.wordCount} words)")
    *   case Left(TestNotFound(_)) => println("No incomplete tests found")
    *   case Left(error) => println(s"Error: ${error.message}")
    * }
    *   }}}
    */
  def getLastTest(profileId: String): AppResult[LastTestResponse]

  /** Submits results for a typing test and marks it as completed.
    *
    * This operation:
    *   1. Validates that the test exists and is not already completed
    *   2. Updates the test with the provided results and completion timestamp
    *   3. Calculates and stores performance metrics
    *   4. Returns the completed test data
    *
    * @param testId
    *   The unique identifier of the test
    * @param results
    *   The test results containing accuracy, timing, and error data
    * @return
    *   AppResult containing the completed test response or an error
    *
    * @example
    *   {{{
    * val results = TestResultsRequest(
    *   accuracy = 97.8,
    *   rawAccuracy = 95.2,
    *   testTime = 38500, // milliseconds
    *   errorCount = 2,
    *   errorWordIndices = List(15, 42)
    * )
    * service.submitTestResults("test-123", results).value.map {
    *   case Right(response) =>
    *     println(s"Test completed with ${response.accuracy.get}% accuracy")
    *   case Left(TestAlreadyCompleted(id)) =>
    *     println(s"Test $id was already completed")
    *   case Left(error) => println(s"Error: ${error.message}")
    * }
    *   }}}
    */
  def submitTestResults(
      testId: String,
      results: TestResultsRequest
  ): AppResult[TestResponse]

  /** Retrieves all available dictionaries across all languages.
    *
    * @return
    *   AppResult containing the dictionaries response or an error
    *
    * @example
    *   {{{
    * service.getAllDictionaries().value.map {
    *   case Right(response) =>
    *     println(s"Available dictionaries: ${response.dictionaries.map(_.name).mkString(", ")}")
    *   case Left(error) => println(s"Error: ${error.message}")
    * }
    *   }}}
    */
  def getAllDictionaries(): AppResult[DictionariesResponse]

  /** Retrieves all available modifiers.
    *
    * @return
    *   AppResult containing the modifiers response or an error
    */
  def getAllModifiers(): AppResult[ModifiersResponse]

  /** Retrieves all available mergers.
    *
    * @return
    *   AppResult containing the mergers response or an error
    */
  def getAllMergers(): AppResult[MergersResponse]

/** Companion object for TypingTestService providing factory methods.
  */
object TypingTestService:

  /** Creates a new TypingTestService instance with the provided repositories.
    *
    * @param profileRepository
    *   Repository for user profile operations
    * @param dictionaryRepository
    *   Repository for dictionary and language operations
    * @param typingTestRepository
    *   Repository for typing test persistence
    * @param statisticsRepository
    *   Repository for statistics data persistence
    * @return
    *   A configured TypingTestService instance
    *
    * @example
    *   {{{
    * val service = TypingTestService(
    *   profileRepository = mongoProfileRepo,
    *   dictionaryRepository = fileDictionaryRepo,
    *   typingTestRepository = mongoTestRepo,
    *   statisticsRepository = mongoStatsRepo
    * )
    *   }}}
    */
  def apply(
      profileRepository: ProfileRepository,
      dictionaryRepository: DictionaryRepository,
      typingTestRepository: TypingTestRepository,
      statisticsRepository: StatisticsRepository
  ): TypingTestService =
    TypingTestServiceImpl(
      profileRepository,
      dictionaryRepository,
      typingTestRepository,
      statisticsRepository
    )

/** Default implementation of TypingTestService using file-based dictionary
  * loading and the provided repository implementations for data persistence.
  *
  * This implementation:
  *   - Uses FileDictionaryLoader for loading dictionary content
  *   - Handles modifier resolution and application via ModifierResolver
  *   - Manages test lifecycle with proper state transitions
  *   - Provides comprehensive error handling and validation
  *   - Ensures data consistency across repository operations
  *
  * The service coordinates between multiple repositories and handles complex
  * business logic like automatic cleanup of non-completed tests when creating
  * new ones.
  *
  * @param profileRepository
  *   Repository for user profile data persistence
  * @param dictionaryRepository
  *   Repository for dictionary and language data
  * @param typingTestRepository
  *   Repository for typing test data persistence
  * @param statisticsRepository
  *   Repository for statistics data persistence
  */
class TypingTestServiceImpl(
    profileRepository: ProfileRepository,
    dictionaryRepository: DictionaryRepository,
    typingTestRepository: TypingTestRepository,
    statisticsRepository: StatisticsRepository
) extends TypingTestService:

  /** Dictionary loader for reading dictionary content from files. Uses
    * FileDictionaryLoader to load word lists and text content.
    */
  private val dictionaryLoader: DictionaryLoader =
    MixedDictionaryLoader(
      Seq(
        ".txt" -> FileDictionaryLoader(),
        ".json" -> JsonDictionaryLoader()
      )
    )

  def requestTest(request: TestRequest): AppResult[TestResponse] =
    for
      _ <- validateModifiers(request.modifiers)
      _ <- validateMergers(request.sources)
      _ <- getProfile(request.profileId)
      _ <- deleteNonCompletedTests(request.profileId)
      sources <- loadSources(request.sources)
      test <- createTypingTest(sources, request)
      persistedTest <- saveTypingTest(
        test,
        request.profileId
      )
      response <- AppResult.pure(
        TypingTestModels.testToResponse(
          test,
          persistedTest.id.get,
          request.profileId
        )
      )
    yield response

  private def loadSources(
      sources: List[SourceWithMerger]
  ): AppResult[List[Dictionary]] =
    AppResult.sequence(
      sources.map(s => getDictionary(s.name))
    )

  private def validateMergers(
      sources: List[SourceWithMerger]
  ): AppResult[Unit] =
    val mergers = sources.drop(1).flatMap(_.merger)
    mergers.find(!MergerResolver.isValidMerger(_)) match
      case Some(invalidMerger) =>
        AppResult.raiseError(
          ValidationError(
            "merger",
            s"Invalid merger: $invalidMerger. Available: ${MergerResolver.getAvailableMergers.mkString(", ")}"
          )
        )
      case None => AppResult.pure(())

  private def createTypingTest(
      dictionaries: List[Dictionary],
      request: TestRequest
  ): AppResult[TypingTest[String] & DefaultContext] =
    AppResult.attemptBlocking(
      IO.blocking {
        val builder = TypingTestFactory
          .create[String]()
          .useLoader(dictionaryLoader)
          .useSource(dictionaries.head, randomized = true)
          .mergeMultiple(
            dictionaries.tail.zip(
              request.sources.tail
                .map(m =>
                  MergerResolver.getMerger(m.merger.getOrElse("concatenate"))
                )
                .filter(_.isDefined)
                .map(_.get)
            )
          )
          .useModifiers(
            request.modifiers
              .map(ModifierResolver.getModifier)
              .filter(_.isDefined)
              .map(_.get)
          )

        val fullTest = builder.build

        TypingTest(
          sources = fullTest.sources,
          modifiers = fullTest.modifiers,
          info = fullTest.info,
          words = fullTest.words.take(request.wordCount)
        )
      }
    )(error =>
      println(error.getMessage)
      TestCreationFailed(error.getMessage)
    )

  def getTestById(testId: String): AppResult[TestResponse] =
    AppResult
      .fromOptionF(
        AppResult
          .attemptBlocking(
            IO.blocking(typingTestRepository.getCompletedById(testId))
          )(error => DatabaseError("test lookup", error.getMessage))
          .value
          .map(_.toOption.flatten),
        TestNotFound(testId)
      )
      .map(TypingTestModels.persistedTestToResponse)

  def getTestsByProfileId(
      profileId: String
  ): AppResult[List[PersistedTypingTest]] =
    AppResult.attemptBlocking(
      IO.blocking(typingTestRepository.getByProfileId(profileId))
    )(error => DatabaseError("tests lookup", error.getMessage))

  def getLastTest(profileId: String): AppResult[LastTestResponse] =
    for
      _ <- getProfile(profileId)
      persistedTest <- AppResult.fromOptionF(
        AppResult
          .attemptBlocking(
            IO.blocking(
              typingTestRepository.getLastNonCompletedByProfileId(profileId)
            )
          )(error => DatabaseError("last test lookup", error.getMessage))
          .value
          .map(_.toOption.flatten),
        TestNotFound("No non-completed test found for profile")
      )
      response <- AppResult.pure(
        TypingTestModels.persistedTestToLastTestResponse(persistedTest)
      )
    yield response

  def submitTestResults(
      testId: String,
      results: TestResultsRequest
  ): AppResult[TestResponse] =
    for
      existingTest <- AppResult.fromOptionF(
        AppResult
          .attemptBlocking(
            IO.blocking(typingTestRepository.get(testId))
          )(error => DatabaseError("test lookup", error.getMessage))
          .value
          .map(_.toOption.flatten),
        TestNotFound(testId)
      )
      _ <- validateTestNotCompleted(existingTest)
      completedTest <- completeTest(existingTest)
      updatedTest <- AppResult.fromOptionF(
        AppResult
          .attemptBlocking(
            IO.blocking(typingTestRepository.update(completedTest))
          )(error => DatabaseError("test update", error.getMessage))
          .value
          .map(_.toOption.flatten),
        TestNotFound(testId)
      )
      _ <- saveTestStatistics(testId, existingTest.profileId, results)
      response <- AppResult.pure(
        TypingTestModels.persistedTestToResponse(updatedTest)
      )
    yield response

  def getAllDictionaries(): AppResult[DictionariesResponse] =
    AppResult
      .attemptBlocking(
        IO.blocking(dictionaryRepository.getAllDictionaries)
      )(error => DatabaseError("dictionaries lookup", error.getMessage))
      .map(TypingTestModels.dictionariesToResponse)

  def getAllModifiers(): AppResult[ModifiersResponse] =
    AppResult.pure(
      TypingTestModels.modifiersToResponse(
        ModifierResolver.getAvailableModifiers
      )
    )

  def getAllMergers(): AppResult[MergersResponse] =
    AppResult.pure(
      TypingTestModels.mergersToResponse(MergerResolver.getAvailableMergers)
    )

  private def validateModifiers(modifierNames: List[String]): AppResult[Unit] =
    modifierNames.find(!ModifierResolver.isValidModifier(_)) match
      case Some(invalidModifier) =>
        AppResult.raiseError(
          InvalidModifier(
            invalidModifier,
            ModifierResolver.getAvailableModifiers
          )
        )
      case None =>
        AppResult.pure(())

  private def getProfile(profileId: String): AppResult[Profile] =
    AppResult.fromOptionF(
      AppResult
        .attemptBlocking(
          IO.blocking(profileRepository.get(profileId))
        )(error => DatabaseError("profile lookup", error.getMessage))
        .value
        .map(_.toOption.flatten),
      ProfileNotFound(profileId)
    )

  private def getDictionary(
      dictionaryName: String
  ): AppResult[Dictionary] =
    AppResult.fromOptionF(
      AppResult
        .attemptBlocking(
          IO.blocking(dictionaryRepository.getDictionaryByName(dictionaryName))
        )(error => DatabaseError("dictionary lookup", error.getMessage))
        .value
        .map(_.toOption.flatten),
      DictionaryNotFound(dictionaryName)
    )

  private def saveTypingTest(
      test: TypingTest[String] & DefaultContext,
      profileId: String
  ): AppResult[PersistedTypingTest] =
    AppResult.attemptBlocking(
      IO.blocking {
        val persistedTest = PersistedTypingTest(
          id = None,
          profileId = profileId,
          testData = test,
          createdAt = com.github.nscala_time.time.Imports.DateTime.now(),
          wordCount = test.words.length
        )
        typingTestRepository.create(persistedTest)
      }
    )(error => TestCreationFailed(s"Failed to save test: ${error.getMessage}"))

  private def deleteNonCompletedTests(profileId: String): AppResult[Unit] =
    AppResult
      .attemptBlocking(
        IO.blocking(
          typingTestRepository.deleteNonCompletedByProfileId(profileId)
        )
      )(error => DatabaseError("delete non-completed tests", error.getMessage))
      .map(_ => ())

  private def validateTestNotCompleted(
      test: PersistedTypingTest
  ): AppResult[Unit] =
    if test.testData.info.completed then
      AppResult.raiseError(TestAlreadyCompleted(test.id.getOrElse("unknown")))
    else AppResult.pure(())

  private def completeTest(
      test: PersistedTypingTest
  ): AppResult[PersistedTypingTest] =
    AppResult.pure(
      test.copy(
        testData = test.testData.copy(
          info = CompletedInfo(
            isCompleted = true,
            completedAt = Some(DateTime.now())
          )
        )
      )
    )

  private def saveTestStatistics(
      testId: String,
      profileId: String,
      results: TestResultsRequest
  ): AppResult[Unit] =
    AppResult
      .attemptBlocking(
        IO.blocking {
          val existingTest = typingTestRepository.get(testId).get
          val wpm = calculateWpm(results.testTime, existingTest.wordCount)
          val statistics = analytics.model.TestStatistics(
            testId = testId,
            userId = profileId,
            wpm = wpm,
            accuracy = results.accuracy,
            errors = results.errorWordIndices,
            timestamp = System.currentTimeMillis()
          )
          statisticsRepository.save(statistics)
        }
      )(error => StatisticsSavingFailed(error.getMessage))
      .map(_ => ())

  private def calculateWpm(testTimeMs: Long, wordCount: Int): Double =
    if testTimeMs <= 0 then 0.0
    else (wordCount.toDouble / testTimeMs.toDouble) * 60000.0

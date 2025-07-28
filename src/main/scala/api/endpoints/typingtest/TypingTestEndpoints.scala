package api.endpoints.typingtest

import api.models.common.{CommonModels, ErrorResponse}
import api.models.common.CommonModels.given
import api.models.typingtest.TypingTestModels
import api.models.typingtest.TypingTestModels.given
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.jsoniter.*

object TypingTestEndpoints:

  private val errorMapping = oneOf[ErrorResponse](
    oneOfVariantValueMatcher(
      statusCode(StatusCode.BadRequest).and(jsonBody[ErrorResponse])
    ) {
      case (err: ErrorResponse) if err.statusCode == 400 => true
      case _                                             => false
    },
    oneOfVariantValueMatcher(
      statusCode(StatusCode.NotFound).and(jsonBody[ErrorResponse])
    ) {
      case (err: ErrorResponse) if err.statusCode == 404 => true
      case _                                             => false
    },
    oneOfVariantValueMatcher(
      statusCode(StatusCode.UnprocessableEntity).and(jsonBody[ErrorResponse])
    ) {
      case (err: ErrorResponse) if err.statusCode == 422 => true
      case _                                             => false
    },
    oneOfVariantValueMatcher(
      statusCode(StatusCode.InternalServerError).and(jsonBody[ErrorResponse])
    ) {
      case (err: ErrorResponse) if err.statusCode == 500 => true
      case _                                             => false
    },
    oneOfVariantValueMatcher(
      statusCode(StatusCode.ServiceUnavailable).and(jsonBody[ErrorResponse])
    ) {
      case (err: ErrorResponse) if err.statusCode == 503 => true
      case _                                             => false
    }
  )

  private val baseEndpoint = endpoint
    .tag("Typing Test API")
    .errorOut(errorMapping)

  val requestTest: PublicEndpoint[
    api.models.typingtest.TestRequest,
    ErrorResponse,
    api.models.typingtest.TestResponse,
    Any
  ] =
    baseEndpoint.post
      .in("api" / "tests")
      .in(
        jsonBody[api.models.typingtest.TestRequest].description(
          "Test creation request with multiple sources and mergers"
        )
      )
      .out(statusCode(StatusCode.Created))
      .out(
        jsonBody[api.models.typingtest.TestResponse]
          .description("Successfully created test")
      )
      .summary("Request a new typing test with multiple sources and mergers")
      .description(
        "Creates a new typing test with the specified sources, mergers, and modifiers."
      )

  val getTestById: PublicEndpoint[
    String,
    ErrorResponse,
    api.models.typingtest.TestResponse,
    Any
  ] =
    baseEndpoint.get
      .in(
        "api" / "tests" / path[String]("testId").description(
          "Unique test identifier"
        )
      )
      .out(
        jsonBody[api.models.typingtest.TestResponse].description("Test details")
      )
      .summary("Get a typing test by ID")
      .description("Retrieves a specific typing test by its unique identifier")

  val getTestsByProfileId: PublicEndpoint[
    String,
    ErrorResponse,
    api.models.typingtest.TestListResponse,
    Any
  ] =
    baseEndpoint.get
      .in(
        "api" / "profiles" / path[String]("profileId").description(
          "Unique profile identifier"
        ) / "tests"
      )
      .out(
        jsonBody[api.models.typingtest.TestListResponse]
          .description("List of tests for the profile")
      )
      .summary("Get all tests for a profile")
      .description("Retrieves all typing tests for a specific user profile")

  val getLastTest: PublicEndpoint[
    String,
    ErrorResponse,
    api.models.typingtest.LastTestResponse,
    Any
  ] =
    baseEndpoint.get
      .in(
        "api" / "profiles" / path[String]("profileId").description(
          "Profile identifier"
        ) / "last-test"
      )
      .out(
        jsonBody[api.models.typingtest.LastTestResponse]
          .description("Last non-completed test")
      )
      .summary("Get the last non-completed test for a profile")
      .description(
        "Retrieves the most recent non-completed typing test for a specific profile"
      )

  val submitTestResults: PublicEndpoint[
    (String, api.models.typingtest.TestResultsRequest),
    ErrorResponse,
    api.models.typingtest.TestResponse,
    Any
  ] =
    baseEndpoint.put
      .in(
        "api" / "tests" / path[String]("testId").description(
          "Test identifier"
        ) / "results"
      )
      .in(
        jsonBody[api.models.typingtest.TestResultsRequest]
          .description("Test results data")
      )
      .out(
        jsonBody[api.models.typingtest.TestResponse]
          .description("Updated test with results")
      )
      .summary("Submit test results")
      .description(
        "Submits the results for a typing test and marks it as completed"
      )

  val getAllDictionaries: PublicEndpoint[
    Unit,
    ErrorResponse,
    api.models.typingtest.DictionariesResponse,
    Any
  ] =
    baseEndpoint.get
      .in("api" / "dictionaries")
      .out(
        jsonBody[api.models.typingtest.DictionariesResponse].description(
          "List of all available dictionaries"
        )
      )
      .summary("Get all available dictionaries")
      .description("Retrieves all available dictionaries")

  val getAllModifiers: PublicEndpoint[
    Unit,
    ErrorResponse,
    api.models.typingtest.ModifiersResponse,
    Any
  ] =
    baseEndpoint.get
      .in("api" / "modifiers")
      .out(
        jsonBody[api.models.typingtest.ModifiersResponse].description(
          "List of all available modifiers"
        )
      )
      .summary("Get all available modifiers")
      .description(
        "Retrieves all available word modifiers with their descriptions"
      )

  val getAllMergers: PublicEndpoint[
    Unit,
    ErrorResponse,
    api.models.typingtest.MergersResponse,
    Any
  ] =
    baseEndpoint.get
      .in("api" / "mergers")
      .out(
        jsonBody[api.models.typingtest.MergersResponse].description(
          "List of all available mergers"
        )
      )
      .summary("Get all available mergers")
      .description(
        "Retrieves all available source mergers with their descriptions"
      )

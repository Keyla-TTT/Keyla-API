package api.endpoints

import api.models.*
import api.models.ApiModels.given
import config.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.jsoniter.*

/** Defines all REST API endpoints for the Keyla typing test application.
  *
  * This object contains endpoint definitions using the Tapir library for:
  *   - User profile management
  *   - Typing test creation and management
  *   - Dictionary and language operations
  *   - Configuration management (git-like config system)
  *
  * All endpoints include proper error handling, documentation, and OpenAPI
  * specification support. The configuration endpoints implement a git-like
  * configuration management system allowing runtime configuration changes with
  * automatic component reinitialization.
  *
  * @example
  *   {{{
  * // Usage in server setup:
  * val routes = ApiEndpoints.getAllEndpoints.map(_.serverLogic(controller.handleRequest))
  *   }}}
  */
object ApiEndpoints:

  /** Common error response mappings for all endpoints. Maps different HTTP
    * status codes to appropriate error response types.
    */
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

  /** Base endpoint definition with common error handling and tagging. All
    * specific endpoints extend this base to inherit consistent error handling.
    */
  private val baseEndpoint = endpoint
    .tag("Typing Test API")
    .errorOut(errorMapping)

  val createProfile: PublicEndpoint[
    CreateProfileRequest,
    ErrorResponse,
    ProfileResponse,
    Any
  ] =
    baseEndpoint.post
      .in("api" / "profiles")
      .in(
        jsonBody[CreateProfileRequest].description("Profile creation request")
      )
      .out(statusCode(StatusCode.Created))
      .out(
        jsonBody[ProfileResponse].description("Successfully created profile")
      )
      .summary("Create a new user profile")
      .description("Creates a new user profile with the provided information")

  val getAllProfiles
      : PublicEndpoint[Unit, ErrorResponse, ProfileListResponse, Any] =
    baseEndpoint.get
      .in("api" / "profiles")
      .out(jsonBody[ProfileListResponse].description("List of all profiles"))
      .summary("Get all user profiles")
      .description("Retrieves all user profiles in the system")

  val requestTest
      : PublicEndpoint[TestRequest, ErrorResponse, TestResponse, Any] =
    baseEndpoint.post
      .in("api" / "tests")
      .in(jsonBody[TestRequest].description("Test creation request"))
      .out(statusCode(StatusCode.Created))
      .out(jsonBody[TestResponse].description("Successfully created test"))
      .summary("Request a new typing test")
      .description("Creates a new typing test with the specified parameters")

  val getTestById: PublicEndpoint[String, ErrorResponse, TestResponse, Any] =
    baseEndpoint.get
      .in(
        "api" / "tests" / path[String]("testId").description(
          "Unique test identifier"
        )
      )
      .out(jsonBody[TestResponse].description("Test details"))
      .summary("Get a typing test by ID")
      .description("Retrieves a specific typing test by its unique identifier")

  val getTestsByProfileId
      : PublicEndpoint[String, ErrorResponse, TestListResponse, Any] =
    baseEndpoint.get
      .in(
        "api" / "profiles" / path[String]("profileId").description(
          "Unique profile identifier"
        ) / "tests"
      )
      .out(
        jsonBody[TestListResponse].description("List of tests for the profile")
      )
      .summary("Get all tests for a profile")
      .description("Retrieves all typing tests for a specific user profile")

  val getTestsByLanguage
      : PublicEndpoint[String, ErrorResponse, TestListResponse, Any] =
    baseEndpoint.get
      .in(
        "api" / "tests" / "language" / path[String]("language").description(
          "Language code"
        )
      )
      .out(
        jsonBody[TestListResponse].description("List of tests for the language")
      )
      .summary("Get tests by language")
      .description("Retrieves all typing tests for a specific language")

  val getLastTest
      : PublicEndpoint[String, ErrorResponse, LastTestResponse, Any] =
    baseEndpoint.get
      .in(
        "api" / "profiles" / path[String]("profileId").description(
          "Profile identifier"
        ) / "last-test"
      )
      .out(jsonBody[LastTestResponse].description("Last non-completed test"))
      .summary("Get the last non-completed test for a profile")
      .description(
        "Retrieves the most recent non-completed typing test for a specific profile"
      )

  val submitTestResults: PublicEndpoint[
    (String, TestResultsRequest),
    ErrorResponse,
    TestResponse,
    Any
  ] =
    baseEndpoint.put
      .in(
        "api" / "tests" / path[String]("testId").description(
          "Test identifier"
        ) / "results"
      )
      .in(jsonBody[TestResultsRequest].description("Test results data"))
      .out(jsonBody[TestResponse].description("Updated test with results"))
      .summary("Submit test results")
      .description(
        "Submits the results for a typing test and marks it as completed"
      )

  val getAllDictionaries
      : PublicEndpoint[Unit, ErrorResponse, DictionariesResponse, Any] =
    baseEndpoint.get
      .in("api" / "dictionaries")
      .out(
        jsonBody[DictionariesResponse].description(
          "List of all available dictionaries"
        )
      )
      .summary("Get all available dictionaries")
      .description("Retrieves all available dictionaries across all languages")

  val getLanguages
      : PublicEndpoint[Unit, ErrorResponse, LanguagesResponse, Any] =
    baseEndpoint.get
      .in("api" / "languages")
      .out(
        jsonBody[LanguagesResponse].description(
          "List of languages with their available dictionaries"
        )
      )
      .summary("Get all languages and their dictionaries")
      .description(
        "Retrieves all available languages with their corresponding dictionaries"
      )

  val getDictionariesByLanguage
      : PublicEndpoint[String, ErrorResponse, DictionariesResponse, Any] =
    baseEndpoint.get
      .in(
        "api" / "languages" / path[String]("language").description(
          "Language code"
        ) / "dictionaries"
      )
      .out(
        jsonBody[DictionariesResponse].description(
          "List of dictionaries for the language"
        )
      )
      .summary("Get dictionaries for a specific language")
      .description(
        "Retrieves all available dictionaries for a specific language"
      )

  val saveStatistics: PublicEndpoint[
    SaveStatisticsRequest,
    ErrorResponse,
    StatisticsResponse,
    Any
  ] =
    baseEndpoint.post
      .in(
        "api" / "stats"
      )
      .in(jsonBody[SaveStatisticsRequest].description("Test statistics data"))
      .out(
        jsonBody[StatisticsResponse].description(
          "Successfully saved test statistics"
        )
      )
      .summary("Save test statistics")
      .description(
        "Saves the provided test's statistics for a specific user profile"
      )

  val getAllProfileStatistics =
    baseEndpoint.get
      .in(
        "api" / "stats" / path[String]("testId").description(
          "Test identifier"
        )
      )
      .out(
        jsonBody[ProfileListResponse].description(
          "List of all profile statistics"
        )
      )
      .summary("Get all profile statistics")
      .description(
        "Retrieves all available statistics for a specific user profile"
      )

  // Configuration endpoints - Git-like configuration management

  /** Lists all available configuration entries with their metadata. Equivalent
    * to `git config --list` but with additional metadata.
    *
    * GET /api/config
    *
    * @return
    *   ConfigListResponse containing all configuration entries with
    *   descriptions and types
    */
  val getAllConfigEntries
      : PublicEndpoint[Unit, ErrorResponse, ConfigListResponse, Any] =
    baseEndpoint.get
      .in("api" / "config")
      .out(
        jsonBody[ConfigListResponse].description(
          "List of all configuration entries with metadata including current values, descriptions, data types, and default values"
        )
      )
      .summary("List all configuration entries")
      .description(
        "Retrieves all available configuration entries with their current values and metadata. Similar to 'git config --list' but includes descriptions, data types, and default values for each entry."
      )

  /** Gets a specific configuration entry by section and key. Equivalent to
    * `git config <section>.<key>`.
    *
    * GET /api/config/{section}/{key}
    *
    * @param section
    *   Configuration section (e.g., "server", "database", "dictionary")
    * @param key
    *   Configuration key within the section (e.g., "port", "host")
    * @return
    *   ConfigEntry with the current value and metadata
    */
  val getConfigEntry
      : PublicEndpoint[(String, String), ErrorResponse, ConfigEntry, Any] =
    baseEndpoint.get
      .in(
        "api" / "config" / path[String]("section").description(
          "Configuration section (server, database, dictionary)"
        ) / path[String]("key").description(
          "Configuration key within the section"
        )
      )
      .out(
        jsonBody[ConfigEntry].description(
          "Configuration entry with current value, description, data type, and default value"
        )
      )
      .summary("Get a specific configuration entry")
      .description(
        "Retrieves a specific configuration entry by section and key. Similar to 'git config <section>.<key>' but returns additional metadata."
      )

  /** Updates a configuration entry with a new value. Equivalent to
    * `git config <section>.<key> <value>`.
    *
    * PUT /api/config
    *
    * Automatically handles:
    *   - Value validation and type conversion
    *   - File persistence
    *   - Repository reinitialization for database/dictionary changes
    *   - Server restart notifications for host/port changes
    *
    * @param request
    *   ConfigUpdateRequest containing the key and new value
    * @return
    *   ConfigUpdateResponse with success status, message, and updated
    *   configuration
    */
  val updateConfigEntry: PublicEndpoint[
    ConfigUpdateRequest,
    ErrorResponse,
    ConfigUpdateResponse,
    Any
  ] =
    baseEndpoint.put
      .in("api" / "config")
      .in(
        jsonBody[ConfigUpdateRequest].description(
          "Configuration update request with section, key, and new value"
        )
      )
      .out(
        jsonBody[ConfigUpdateResponse].description(
          "Update result with success status, descriptive message, and complete updated configuration"
        )
      )
      .summary("Update a configuration entry")
      .description(
        "Updates a configuration entry and automatically handles persistence, validation, and component reinitialization. Similar to 'git config <section>.<key> <value>' with automatic side-effect management."
      )

  /** Gets the complete current configuration.
    *
    * GET /api/config/current
    *
    * @return
    *   AppConfig containing the complete current configuration
    */
  val getCurrentConfig: PublicEndpoint[Unit, ErrorResponse, AppConfig, Any] =
    baseEndpoint.get
      .in("api" / "config" / "current")
      .out(
        jsonBody[AppConfig].description(
          "Complete current application configuration including all sections"
        )
      )
      .summary("Get current complete configuration")
      .description(
        "Retrieves the complete current application configuration including database, server, and dictionary settings"
      )

  /** Reloads configuration from the configuration file.
    *
    * POST /api/config/reload
    *
    * Useful for applying changes made directly to the configuration file.
    * Automatically reinitializes all repositories with the new settings.
    *
    * @return
    *   AppConfig containing the reloaded configuration
    */
  val reloadConfig: PublicEndpoint[Unit, ErrorResponse, AppConfig, Any] =
    baseEndpoint.post
      .in("api" / "config" / "reload")
      .out(jsonBody[AppConfig].description("Reloaded configuration from file"))
      .summary("Reload configuration from file")
      .description(
        "Reloads the configuration from the configuration file and reinitializes all components with the new settings"
      )

  /** Resets all configuration to default values.
    *
    * POST /api/config/reset
    *
    * Equivalent to deleting the configuration file and starting fresh. Creates
    * new default configuration, saves it to file, and reinitializes all
    * components.
    *
    * @return
    *   AppConfig containing the reset default configuration
    */
  val resetConfigToDefaults
      : PublicEndpoint[Unit, ErrorResponse, AppConfig, Any] =
    baseEndpoint.post
      .in("api" / "config" / "reset")
      .out(
        jsonBody[AppConfig].description(
          "Reset configuration with all default values"
        )
      )
      .summary("Reset configuration to defaults")
      .description(
        "Resets all configuration to default values, saves to file, and reinitializes all components"
      )

  /** Complete list of all available API endpoints. Used by the server to
    * register all endpoint handlers.
    */
  val getAllEndpoints = List(
    createProfile,
    getAllProfiles,
    requestTest,
    getTestById,
    getTestsByProfileId,
    getTestsByLanguage,
    getLastTest,
    submitTestResults,
    getAllDictionaries,
    getLanguages,
    getDictionariesByLanguage,
    saveStatistics,
    getAllProfileStatistics,
    getAllConfigEntries,
    getConfigEntry,
    updateConfigEntry,
    getCurrentConfig,
    reloadConfig,
    resetConfigToDefaults
  )

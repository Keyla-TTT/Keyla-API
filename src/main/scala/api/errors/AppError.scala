package api.models

import api.models.ErrorResponse
import sttp.model.StatusCode

/** Sealed trait representing all possible application errors. Provides
  * consistent error handling across the entire application with proper HTTP
  * status codes.
  *
  * Each error includes:
  *   - A descriptive message for users/logs
  *   - A stable error code for programmatic handling
  *   - An appropriate HTTP status code for REST API responses
  *
  * @param message
  *   Human-readable error description
  * @param code
  *   Stable error code for programmatic identification
  * @param statusCode
  *   HTTP status code to return in API responses
  */
sealed abstract class AppError(
    val message: String,
    val code: String,
    val statusCode: StatusCode
) extends Product
    with Serializable:
  /** Converts this AppError to an ErrorResponse suitable for JSON
    * serialization. Used by the API layer to return consistent error responses.
    *
    * @return
    *   ErrorResponse containing the error details
    */
  def toErrorResponse: ErrorResponse =
    ErrorResponse(message, code, statusCode.code)

/** Companion object containing all specific error types organized by category.
  * Provides factory methods for creating strongly-typed errors throughout the
  * application.
  */
object AppError:

  // Client errors (4xx)
  case class ProfileNotFound(profileId: String)
      extends AppError(
        s"Profile with id '$profileId' not found",
        "PROFILE_NOT_FOUND",
        StatusCode.NotFound
      )

  case class TestNotFound(testId: String)
      extends AppError(
        s"Test with id '$testId' not found",
        "TEST_NOT_FOUND",
        StatusCode.NotFound
      )

  case class TestAlreadyCompleted(testId: String)
      extends AppError(
        s"Test with id '$testId' is already completed",
        "TEST_ALREADY_COMPLETED",
        StatusCode.BadRequest
      )

  case class LanguageNotSupported(language: String)
      extends AppError(
        s"Language '$language' is not supported",
        "LANGUAGE_NOT_FOUND",
        StatusCode.NotFound
      )

  case class DictionaryNotFound(language: String, dictionaryName: String)
      extends AppError(
        s"Dictionary '$dictionaryName' not found for language '$language'",
        "DICTIONARY_NOT_FOUND",
        StatusCode.NotFound
      )

  case class ValidationError(field: String, reason: String)
      extends AppError(
        s"Validation error for field '$field': $reason",
        "VALIDATION_ERROR",
        StatusCode.UnprocessableEntity
      )

  case class InvalidModifier(modifier: String, availableModifiers: Set[String])
      extends AppError(
        s"Invalid modifier: '$modifier'. Available modifiers: ${availableModifiers.mkString(", ")}",
        "INVALID_MODIFIER",
        StatusCode.UnprocessableEntity
      )

  case class InvalidRequest(reason: String)
      extends AppError(
        s"Invalid request: $reason",
        "INVALID_REQUEST",
        StatusCode.BadRequest
      )

  // Configuration errors (4xx) - Git-like configuration management errors

  /** Error when attempting to access a configuration key that doesn't exist.
    * Similar to git reporting an unknown configuration key.
    *
    * @param section
    *   The configuration section that was requested
    * @param key
    *   The configuration key within the section that was not found
    *
    * @example
    *   {{{
    * // When trying to get a non-existent config:
    * // GET /api/config/nonexistent/key
    * ConfigKeyNotFound("nonexistent", "key")
    *   }}}
    */
  case class ConfigKeyNotFound(section: String, key: String)
      extends AppError(
        s"Configuration key '$section.$key' not found",
        "CONFIG_KEY_NOT_FOUND",
        StatusCode.NotFound
      )

  /** Error when the configuration file cannot be found during reload
    * operations. Indicates the file was deleted or moved after the application
    * started.
    *
    * @param filePath
    *   The absolute path where the configuration file was expected
    *
    * @example
    *   {{{
    * // When trying to reload config but file is missing:
    * // POST /api/config/reload
    * ConfigFileNotFound("/path/to/keyla-config.json")
    *   }}}
    */
  case class ConfigFileNotFound(filePath: String)
      extends AppError(
        s"Configuration file not found at: $filePath",
        "CONFIG_FILE_NOT_FOUND",
        StatusCode.NotFound
      )

  /** Error when a configuration update operation fails due to system issues.
    * This covers file I/O errors, permission issues, or repository
    * initialization failures.
    *
    * @param section
    *   The configuration section being updated
    * @param key
    *   The configuration key being updated
    * @param reason
    *   Detailed reason for the failure
    *
    * @example
    *   {{{
    * // When file system doesn't allow writing the config file:
    * ConfigUpdateFailed("server", "port", "Permission denied writing to config file")
    *   }}}
    */
  case class ConfigUpdateFailed(section: String, key: String, reason: String)
      extends AppError(
        s"Failed to update configuration '$section.$key': $reason",
        "CONFIG_UPDATE_FAILED",
        StatusCode.BadRequest
      )

  /** Error when a configuration value doesn't meet validation requirements.
    * Occurs when the provided value cannot be converted to the expected type or
    * doesn't meet business logic constraints.
    *
    * @param section
    *   The configuration section being updated
    * @param key
    *   The configuration key being updated
    * @param value
    *   The invalid value that was provided
    * @param reason
    *   Specific reason why the value is invalid
    *
    * @example
    *   {{{
    * // When trying to set an invalid port number:
    * InvalidConfigValue("server", "port", "invalid_port", "For input string: \"invalid_port\"")
    *
    * // When trying to set a port outside valid range:
    * InvalidConfigValue("server", "port", "99999", "Port must be between 1 and 65535")
    *   }}}
    */
  case class InvalidConfigValue(
      section: String,
      key: String,
      value: String,
      reason: String
  ) extends AppError(
        s"Invalid value '$value' for configuration '$section.$key': $reason",
        "INVALID_CONFIG_VALUE",
        StatusCode.BadRequest
      )

  // Server errors (5xx)
  case class ProfileCreationFailed(reason: String)
      extends AppError(
        s"Failed to create profile: $reason",
        "PROFILE_CREATION_ERROR",
        StatusCode.InternalServerError
      )

  case class TestCreationFailed(reason: String)
      extends AppError(
        s"Failed to create test: $reason",
        "TEST_CREATION_ERROR",
        StatusCode.InternalServerError
      )

  case class DatabaseError(operation: String, reason: String)
      extends AppError(
        s"Database error during $operation: $reason",
        "DATABASE_ERROR",
        StatusCode.InternalServerError
      )

  case class FileSystemError(operation: String, reason: String)
      extends AppError(
        s"File system error during $operation: $reason",
        "FILESYSTEM_ERROR",
        StatusCode.InternalServerError
      )

  case class UnexpectedError(reason: String)
      extends AppError(
        s"Unexpected error: $reason",
        "INTERNAL_ERROR",
        StatusCode.InternalServerError
      )

  case class StatisticsSavingFailed(reason: String)
      extends AppError(
        s"Failed to saving Statistics: $reason",
        "STATISTICS_SAVING_ERROR",
        StatusCode.InternalServerError
      )

  // Service Unavailable (503)
  case class ServiceUnavailable(service: String, reason: String)
      extends AppError(
        s"Service '$service' is temporarily unavailable: $reason",
        "SERVICE_UNAVAILABLE",
        StatusCode.ServiceUnavailable
      )

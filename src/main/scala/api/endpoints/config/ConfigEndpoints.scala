package api.endpoints.config

import api.models.common.{CommonModels, ErrorResponse}
import api.models.common.CommonModels.given
import api.models.config.ConfigModels
import api.models.config.ConfigModels.given
import api.services.{
  ConfigEntry,
  ConfigListResponse,
  ConfigUpdateResponse,
  SimpleConfigUpdateRequest
}
import config.*
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.jsoniter.*
import sttp.tapir.generic.auto.given

object ConfigEndpoints:

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
    .tag("Configuration API")
    .errorOut(errorMapping)

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

  val getConfigEntry: PublicEndpoint[String, ErrorResponse, ConfigEntry, Any] =
    baseEndpoint.get
      .in(
        "api" / "config" / path[String]("key").description(
          "Configuration key in dot notation format (e.g., server.port, database.useMongodb)"
        )
      )
      .out(
        jsonBody[ConfigEntry].description(
          "Configuration entry with current value, description, data type, and default value"
        )
      )
      .summary("Get a specific configuration entry")
      .description(
        "Retrieves a specific configuration entry by dot notation key. Similar to 'git config <section>.<key>' but uses simplified dot notation format."
      )

  val updateConfigEntry: PublicEndpoint[
    SimpleConfigUpdateRequest,
    ErrorResponse,
    ConfigUpdateResponse,
    Any
  ] =
    baseEndpoint.put
      .in("api" / "config")
      .in(
        jsonBody[SimpleConfigUpdateRequest].description(
          "Simplified configuration update request with key in dot notation and new value"
        )
      )
      .out(
        jsonBody[ConfigUpdateResponse].description(
          "Update result with success status, descriptive message, and complete updated configuration"
        )
      )
      .summary("Update a configuration entry")
      .description(
        "Updates a configuration entry using simplified dot notation keys and automatically handles persistence, validation, and component reinitialization."
      )

  val getCurrentConfig: PublicEndpoint[Unit, ErrorResponse, AppConfig, Any] =
    baseEndpoint.get
      .in("api" / "current-config")
      .out(
        jsonBody[AppConfig].description(
          "Complete current application configuration including all sections"
        )
      )
      .summary("Get current complete configuration")
      .description(
        "Retrieves the complete current application configuration including database, server, and dictionary settings"
      )

  val reloadConfig: PublicEndpoint[Unit, ErrorResponse, AppConfig, Any] =
    baseEndpoint.post
      .in("api" / "config" / "reload")
      .out(jsonBody[AppConfig].description("Reloaded configuration from file"))
      .summary("Reload configuration from file")
      .description(
        "Reloads the configuration from the configuration file and reinitializes all components with the new settings"
      )

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

package api.controllers

import api.models.AppError
import cats.effect.IO
import config.*

/** Controller for handling configuration management REST API endpoints.
  * Provides a thin layer over ConfigurationService for HTTP API operations.
  *
  * This controller implements git-like configuration management functionality:
  *   - List all configuration entries (like `git config --list`)
  *   - Get specific configuration values (like `git config user.name`)
  *   - Set configuration values (like `git config user.name "John Doe"`)
  *   - Reload configuration from file
  *   - Reset to default values
  *   - Get complete current configuration
  *
  * @param configService
  *   The configuration service that handles the actual configuration logic
  *
  * @example
  *   {{{
  * val configService = // ... create configuration service
  * val controller = ConfigurationController(configService)
  *
  * // List all config entries
  * controller.getAllConfigEntries().flatMap {
  *   case Right(response) => IO.println(s"Found ${response.entries.length} config entries")
  *   case Left(error) => IO.println(s"Error: ${error.message}")
  * }
  *   }}}
  */
class ConfigurationController(configService: ConfigurationService):

  /** Gets all available configuration entries with their metadata. Equivalent
    * to `git config --list` but with additional metadata like descriptions and
    * types.
    *
    * @return
    *   IO effect containing either an error or the complete list of
    *   configuration entries
    *
    * @example
    *   {{{
    * controller.getAllConfigEntries().map {
    *   case Right(response) =>
    *     response.entries.foreach(entry =>
    *       println(s"${entry.key.section}.${entry.key.key} = ${entry.value}")
    *     )
    *   case Left(error) => println(s"Failed to get config: ${error.message}")
    * }
    *   }}}
    */
  def getAllConfigEntries(): IO[Either[AppError, ConfigListResponse]] =
    configService.getAllConfigEntries().map(Right(_))

  /** Gets a specific configuration entry by section and key name. Equivalent to
    * `git config <section>.<key>`.
    *
    * @param section
    *   The configuration section (e.g., "server", "database", "dictionary")
    * @param key
    *   The configuration key within the section (e.g., "port", "host")
    * @return
    *   IO effect containing either an error or the configuration entry
    *
    * @example
    *   {{{
    * controller.getConfigEntry("server", "port").map {
    *   case Right(entry) => println(s"Server port: ${entry.value}")
    *   case Left(AppError.ConfigKeyNotFound(section, key)) =>
    *     println(s"Configuration key $section.$key not found")
    *   case Left(error) => println(s"Error: ${error.message}")
    * }
    *   }}}
    */
  def getConfigEntry(
      section: String,
      key: String
  ): IO[Either[AppError, config.ConfigEntry]] =
    configService.getConfigEntry(ConfigKey(section, key))

  /** Updates a configuration entry with a new value. Equivalent to
    * `git config <section>.<key> <value>`.
    *
    * Automatically:
    *   - Validates the new value according to the expected data type
    *   - Saves the updated configuration to file
    *   - Reinitializes repositories if the change affects database/dictionary
    *     settings
    *   - Provides feedback about whether a server restart is required
    *
    * @param request
    *   The update request containing the configuration key and new value
    * @return
    *   IO effect containing either an error or the update response with
    *   feedback
    *
    * @example
    *   {{{
    * val updateRequest = ConfigUpdateRequest(
    *   key = ConfigKey("server", "port"),
    *   value = "9090"
    * )
    *
    * controller.updateConfigEntry(updateRequest).map {
    *   case Right(response) =>
    *     println(s"Update successful: ${response.message}")
    *     if (response.message.contains("restart required")) {
    *       println("Please restart the server for changes to take effect")
    *     }
    *   case Left(error) => println(s"Update failed: ${error.message}")
    * }
    *   }}}
    */
  def updateConfigEntry(
      request: ConfigUpdateRequest
  ): IO[Either[AppError, ConfigUpdateResponse]] =
    configService.updateConfigEntry(request)

  /** Reloads the configuration from the configuration file. Useful for applying
    * changes made directly to the configuration file.
    *
    * This operation:
    *   - Reads the current configuration file from disk
    *   - Updates the in-memory configuration
    *   - Reinitializes all repositories with the new settings
    *
    * @return
    *   IO effect containing either an error or the reloaded configuration
    *
    * @example
    *   {{{
    * controller.reloadConfig().map {
    *   case Right(config) => println("Configuration reloaded successfully")
    *   case Left(AppError.ConfigFileNotFound(path)) =>
    *     println(s"Configuration file not found at: $path")
    *   case Left(error) => println(s"Reload failed: ${error.message}")
    * }
    *   }}}
    */
  def reloadConfig(): IO[Either[AppError, config.AppConfig]] =
    configService.reloadConfig()

  /** Resets all configuration values to their defaults. Equivalent to deleting
    * the configuration file and starting fresh.
    *
    * This operation:
    *   - Creates a new default configuration
    *   - Saves it to the configuration file
    *   - Updates the in-memory configuration
    *   - Reinitializes all repositories with default settings
    *
    * @return
    *   IO effect containing either an error or the default configuration
    *
    * @example
    *   {{{
    * controller.resetToDefaults().map {
    *   case Right(config) =>
    *     println("Configuration reset to defaults")
    *     println(s"Server will run on port ${config.server.port}")
    *   case Left(error) => println(s"Reset failed: ${error.message}")
    * }
    *   }}}
    */
  def resetToDefaults(): IO[Either[AppError, config.AppConfig]] =
    configService.resetToDefaults()

  /** Gets the complete current configuration. Returns the entire configuration
    * object as currently loaded in memory.
    *
    * @return
    *   IO effect containing either an error or the complete current
    *   configuration
    *
    * @example
    *   {{{
    * controller.getCurrentConfig().map {
    *   case Right(config) =>
    *     println(s"Server: ${config.server.host}:${config.server.port}")
    *     println(s"Database: ${config.database.databaseName}")
    *     println(s"Dictionaries: ${config.dictionary.basePath}")
    *   case Left(error) => println(s"Error getting config: ${error.message}")
    * }
    *   }}}
    */
  def getCurrentConfig(): IO[Either[AppError, config.AppConfig]] =
    configService.getCurrentConfig().map(Right(_))

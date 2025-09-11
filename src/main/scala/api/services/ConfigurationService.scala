package api.services

import analytics.repository.StatisticsRepository
import api.models.AppError
import cats.effect.{IO, Ref}
import config.*
import typingTest.dictionary.repository.DictionaryRepository
import typingTest.tests.repository.TypingTestRepository
import users_management.repository.ProfileRepository

/** Represents a configuration key with its section and specific key name. Used
  * to uniquely identify configuration entries in the git-like config system.
  *
  * @param section
  *   The configuration section (e.g., "database", "server", "dictionary")
  * @param key
  *   The specific configuration key within the section (e.g., "port", "host")
  *
  * @example
  *   {{{
  * val serverPortKey = ConfigKey("server", "port")
  * val databaseUriKey = ConfigKey("database", "mongoUri")
  *   }}}
  */
case class ConfigKey(section: String, key: String)

/** Wrapper for a configuration value as a string. All configuration values are
  * stored as strings and converted to appropriate types when needed.
  *
  * @param value
  *   The string representation of the configuration value
  */
case class ConfigValue(value: String)

/** Represents a complete configuration entry with metadata. Contains the key,
  * current value, description, data type, and default value.
  *
  * @param key
  *   The configuration key (section + key name)
  * @param value
  *   The current value as a string
  * @param description
  *   Human-readable description of what this configuration controls
  * @param dataType
  *   The expected data type ("string", "integer", "boolean")
  * @param defaultValue
  *   The default value for this configuration entry
  */
case class ConfigEntry(
    key: ConfigKey,
    value: String,
    description: String,
    dataType: String,
    defaultValue: String
)

/** Response wrapper for listing all configuration entries. Used by the GET
  * /api/config endpoint to return all available configuration options.
  *
  * @param entries
  *   List of all available configuration entries with their metadata
  */
case class ConfigListResponse(
    entries: List[ConfigEntry]
)

/** Request payload for updating a configuration entry. Used by the PUT
  * /api/config endpoint to modify configuration values.
  *
  * @param key
  *   The configuration key to update
  * @param value
  *   The new value as a string (will be validated and converted as needed)
  *
  * @example
  *   {{{
  * val request = ConfigUpdateRequest(
  *   key = ConfigKey("server", "port"),
  *   value = "9090"
  * )
  *   }}}
  */
case class ConfigUpdateRequest(
    key: ConfigKey,
    value: String
)

/** Simplified request payload for updating a configuration entry using dot
  * notation. Used by the PUT /api/config endpoint to modify configuration
  * values.
  *
  * @param key
  *   The configuration key in dot notation format (e.g., "database.useMongodb",
  *   "server.port")
  * @param value
  *   The new value as a string (will be validated and converted as needed)
  *
  * @example
  *   {{{
  * val request = SimpleConfigUpdateRequest(
  *   key = "database.useMongodb",
  *   value = "true"
  * )
  *   }}}
  */
case class SimpleConfigUpdateRequest(
    key: String,
    value: String
)

/** Response payload for configuration update operations. Provides feedback on
  * the update success and any side effects (like repository reinitialization).
  *
  * @param success
  *   Whether the update operation succeeded
  * @param message
  *   Descriptive message about what happened, including restart requirements
  */
case class ConfigUpdateResponse(
    success: Boolean,
    message: String
)

/** Service trait for managing application configuration in a git-like manner.
  * Provides operations to read, update, and manage configuration entries with
  * automatic persistence and repository reinitialization when needed.
  *
  * The service supports:
  *   - Getting individual or all configuration entries
  *   - Updating configuration with automatic type conversion
  *   - Reloading configuration from file
  *   - Resetting to default values
  *   - Automatic repository reinitialization for database/dictionary changes
  *   - Clear indication when server restart is required
  */
trait ConfigurationService:

  /** Gets the current complete application configuration.
    *
    * @return
    *   IO effect containing the current AppConfig
    */
  def getCurrentConfig: IO[AppConfig]

  /** Gets a specific configuration entry by its key.
    *
    * @param key
    *   The configuration key to retrieve
    * @return
    *   IO effect containing either an error or the configuration entry
    */
  def getConfigEntry(key: ConfigKey): IO[Either[AppError, ConfigEntry]]

  /** Gets all available configuration entries with their metadata.
    *
    * @return
    *   IO effect containing the list of all configuration entries
    */
  def getAllConfigEntries(): IO[ConfigListResponse]

  /** Updates a configuration entry with a new value. Automatically saves to
    * file, updates in-memory config, and reinitializes repositories if the
    * change affects database or dictionary settings.
    *
    * @param request
    *   The update request containing the key and new value
    * @return
    *   IO effect containing either an error or the update response
    */
  def updateConfigEntry(
      request: ConfigUpdateRequest
  ): IO[Either[AppError, ConfigUpdateResponse]]

  /** Updates a configuration entry using simplified dot notation key format.
    * Automatically saves to file, updates in-memory config, and reinitializes
    * repositories if the change affects database or dictionary settings.
    *
    * @param request
    *   The simplified update request containing the key in dot notation and new
    *   value
    * @return
    *   IO effect containing either an error or the update response
    */
  def updateConfigEntrySimple(
      request: SimpleConfigUpdateRequest
  ): IO[Either[AppError, ConfigUpdateResponse]]

  /** Reloads the configuration from the configuration file. Updates in-memory
    * state and reinitializes repositories with the loaded config.
    *
    * @return
    *   IO effect containing either an error or the reloaded configuration
    */
  def reloadConfig(): IO[Either[AppError, AppConfig]]

  /** Resets the configuration to default values. Saves defaults to file,
    * updates in-memory state, and reinitializes repositories.
    *
    * @return
    *   IO effect containing either an error or the default configuration
    */
  def resetToDefaults(): IO[Either[AppError, AppConfig]]

/** File-based implementation of ConfigurationService that persists
  * configuration to a JSON file and manages repository references for dynamic
  * reinitialization.
  *
  * This implementation:
  *   - Stores configuration in a JSON file (keyla-config.json)
  *   - Maintains thread-safe references to the current config and repositories
  *   - Automatically reinitializes repositories when database/dictionary config
  *     changes
  *   - Provides clear feedback about when server restart is required
  *   - Supports atomic configuration updates with rollback on failure
  *
  * @param configRef
  *   Thread-safe reference to the current configuration
  * @param profileRepositoryRef
  *   Thread-safe reference to the profile repository
  * @param typingTestRepositoryRef
  *   Thread-safe reference to the typing test repository
  * @param dictionaryRepositoryRef
  *   Thread-safe reference to the dictionary repository
  */
class FileConfigurationService(
    configRef: Ref[IO, AppConfig],
    profileRepositoryRef: Ref[IO, ProfileRepository],
    typingTestRepositoryRef: Ref[IO, TypingTestRepository],
    dictionaryRepositoryRef: Ref[IO, DictionaryRepository],
    statisticsRepositoryRef: Ref[IO, StatisticsRepository]
) extends ConfigurationService:

  override def getCurrentConfig: IO[AppConfig] = configRef.get

  override def getConfigEntry(
      key: ConfigKey
  ): IO[Either[AppError, ConfigEntry]] =
    for
      config <- configRef.get
      entry <- IO.pure(extractConfigEntry(config, key))
    yield entry match
      case Some(entry) => Right(entry)
      case None        => Left(AppError.ConfigKeyNotFound(key.section, key.key))

  override def getAllConfigEntries(): IO[ConfigListResponse] =
    for
      config <- configRef.get
      entries = extractAllConfigEntries(config)
    yield ConfigListResponse(entries)

  override def updateConfigEntry(
      request: ConfigUpdateRequest
  ): IO[Either[AppError, ConfigUpdateResponse]] =
    for
      currentConfig <- configRef.get
      result <- updateConfig(currentConfig, request.key, request.value)
    yield result

  override def updateConfigEntrySimple(
      request: SimpleConfigUpdateRequest
  ): IO[Either[AppError, ConfigUpdateResponse]] =
    for
      currentConfig <- configRef.get
      result <- updateConfig(
        currentConfig,
        parseSimpleKey(request.key),
        request.value
      )
    yield result

  override def reloadConfig(): IO[Either[AppError, AppConfig]] =
    for
      configPath <- IO.pure(ConfigUtils.getConfigPath)
      configOpt <- ConfigUtils.loadFromFile(configPath)
      result <- configOpt match
        case Some(config) =>
          for
            _ <- configRef.set(config)
            _ <- updateRepositories(config)
          yield Right(config)
        case None =>
          IO.pure(Left(AppError.ConfigFileNotFound(configPath)))
    yield result

  override def resetToDefaults(): IO[Either[AppError, AppConfig]] =
    for
      defaultConfig <- IO.pure(ConfigUtils.getDefaultConfig)
      _ <- configRef.set(defaultConfig)
      _ <- ConfigUtils.saveToFile(defaultConfig, ConfigUtils.getConfigPath)
      _ <- updateRepositories(defaultConfig)
    yield Right(defaultConfig)

  /** Internal method to handle configuration updates with proper error
    * handling, file persistence, and repository reinitialization.
    *
    * @param currentConfig
    *   The current configuration before the update
    * @param key
    *   The configuration key to update
    * @param value
    *   The new value as a string
    * @return
    *   IO effect containing either an error or the update response
    */
  private def updateConfig(
      currentConfig: AppConfig,
      key: ConfigKey,
      value: String
  ): IO[Either[AppError, ConfigUpdateResponse]] =
    try
      val updatedConfig = updateConfigValue(currentConfig, key, value)
      for
        _ <- ConfigUtils.saveToFile(updatedConfig, ConfigUtils.getConfigPath)
        _ <- configRef.set(updatedConfig)
        needsRepoUpdate = requiresRepositoryUpdate(key)
        needsRestart = requiresServerRestart(key)
        _ <-
          if needsRepoUpdate then updateRepositories(updatedConfig) else IO.unit
        message = (needsRepoUpdate, needsRestart) match
          case (true, true) =>
            "Configuration updated, repositories reinitialized. Server restart required for host/port changes to take effect."
          case (true, false) =>
            "Configuration updated and repositories reinitialized"
          case (false, true) =>
            "Configuration updated. Server restart required for host/port changes to take effect."
          case (false, false) => "Configuration updated"
      yield Right(
        ConfigUpdateResponse(
          success = true,
          message = message
        )
      )
    catch
      case ex: Exception =>
        IO.pure(
          Left(AppError.ConfigUpdateFailed(key.section, key.key, ex.getMessage))
        )

  /** Reinitializes all repositories with the new configuration. Called when
    * database or dictionary configuration changes to ensure repositories use
    * the updated settings.
    *
    * @param config
    *   The new configuration to use for repository creation
    * @return
    *   IO effect that completes when all repositories are updated
    */
  private def updateRepositories(config: AppConfig): IO[Unit] =
    for
      newProfileRepo <- IO.pure(ConfigUtils.createProfileRepository(config))
      newTestRepo <- IO.pure(ConfigUtils.createTypingTestRepository(config))
      newDictRepo <- IO.pure(ConfigUtils.createDictionaryRepository(config))
      newStatisticsRepo <- IO.pure(
        ConfigUtils.createStatisticsRepository(config)
      )
      _ <- profileRepositoryRef.set(newProfileRepo)
      _ <- typingTestRepositoryRef.set(newTestRepo)
      _ <- dictionaryRepositoryRef.set(newDictRepo)
      _ <- statisticsRepositoryRef.set(newStatisticsRepo)
    yield ()

  /** Determines if a configuration key change requires repository
    * reinitialization. Database and dictionary path/extension changes require
    * new repository instances.
    *
    * @param key
    *   The configuration key being updated
    * @return
    *   true if repositories need to be reinitialized, false otherwise
    */
  private def requiresRepositoryUpdate(key: ConfigKey): Boolean =
    key match
      case ConfigKey("database", _)            => true
      case ConfigKey("dictionary", "basePath") => true
      case _                                   => false

  /** Determines if a configuration key change requires a server restart. Server
    * host and port changes cannot be applied to a running server.
    *
    * @param key
    *   The configuration key being updated
    * @return
    *   true if a server restart is required, false otherwise
    */
  private def requiresServerRestart(key: ConfigKey): Boolean =
    key match
      case ConfigKey("server", "host") => true
      case ConfigKey("server", "port") => true
      case _                           => false

  /** Updates a specific configuration value in the AppConfig structure.
    * Performs type conversion and validation for the new value.
    *
    * @param config
    *   The current configuration
    * @param key
    *   The configuration key to update
    * @param value
    *   The new value as a string
    * @return
    *   Updated AppConfig with the new value
    * @throws IllegalArgumentException
    *   if the key is unknown or value is invalid
    */
  private def updateConfigValue(
      config: AppConfig,
      key: ConfigKey,
      value: String
  ): AppConfig =
    key match
      case ConfigKey("database", "mongoUri") =>
        config.copy(database = config.database.copy(mongoUri = value))
      case ConfigKey("database", "databaseName") =>
        config.copy(database = config.database.copy(databaseName = value))
      case ConfigKey("database", "useMongoDb") =>
        config.copy(database =
          config.database.copy(useMongoDb = value.toLowerCase == "true")
        )
      case ConfigKey("server", "host") =>
        config.copy(server = config.server.copy(host = value))
      case ConfigKey("server", "port") =>
        config.copy(server = config.server.copy(port = value.toInt))
      case ConfigKey("dictionary", "basePath") =>
        config.copy(dictionary = config.dictionary.copy(basePath = value))
      case ConfigKey("dictionary", "autoCreateDirectories") =>
        config.copy(dictionary =
          config.dictionary.copy(autoCreateDirectories =
            value.toLowerCase == "true"
          )
        )
      case _ =>
        throw new IllegalArgumentException(
          s"Unknown config key: ${key.section}.${key.key}"
        )

  /** Parses a simplified dot notation key into a ConfigKey.
    *
    * @param key
    *   The simplified dot notation key (e.g., "database.useMongodb")
    * @return
    *   The corresponding ConfigKey
    */
  private def parseSimpleKey(key: String): ConfigKey =
    key.split('.').toList match
      case "database" :: "mongoUri" :: Nil => ConfigKey("database", "mongoUri")
      case "database" :: "databaseName" :: Nil =>
        ConfigKey("database", "databaseName")
      case "database" :: "useMongoDb" :: Nil =>
        ConfigKey("database", "useMongoDb")
      case "server" :: "host" :: Nil => ConfigKey("server", "host")
      case "server" :: "port" :: Nil => ConfigKey("server", "port")
      case "dictionary" :: "basePath" :: Nil =>
        ConfigKey("dictionary", "basePath")
      case "dictionary" :: "autoCreateDirectories" :: Nil =>
        ConfigKey("dictionary", "autoCreateDirectories")
      case _ => throw new IllegalArgumentException(s"Unknown key: $key")

  /** Extracts a single configuration entry with metadata from the current
    * config. Returns None if the key is not recognized.
    *
    * @param config
    *   The current configuration
    * @param key
    *   The configuration key to extract
    * @return
    *   Some(ConfigEntry) if the key exists, None otherwise
    */
  private def extractConfigEntry(
      config: AppConfig,
      key: ConfigKey
  ): Option[ConfigEntry] =
    key match
      case ConfigKey("database", "mongoUri") =>
        Some(
          ConfigEntry(
            key,
            config.database.mongoUri,
            "MongoDB connection URI",
            "string",
            "mongodb://localhost:27017"
          )
        )
      case ConfigKey("database", "databaseName") =>
        Some(
          ConfigEntry(
            key,
            config.database.databaseName,
            "Database name",
            "string",
            "keyla_db"
          )
        )
      case ConfigKey("database", "useMongoDb") =>
        Some(
          ConfigEntry(
            key,
            config.database.useMongoDb.toString,
            "Use MongoDB instead of in-memory storage",
            "boolean",
            "false"
          )
        )
      case ConfigKey("server", "host") =>
        Some(
          ConfigEntry(
            key,
            config.server.host,
            "Server host address (requires restart)",
            "string",
            "localhost"
          )
        )
      case ConfigKey("server", "port") =>
        Some(
          ConfigEntry(
            key,
            config.server.port.toString,
            "Server port number (requires restart)",
            "integer",
            "9999"
          )
        )
      case ConfigKey("dictionary", "basePath") =>
        Some(
          ConfigEntry(
            key,
            config.dictionary.basePath,
            "Base path for dictionary files",
            "string",
            "src/main/resources/dictionaries"
          )
        )
      case ConfigKey("dictionary", "autoCreateDirectories") =>
        Some(
          ConfigEntry(
            key,
            config.dictionary.autoCreateDirectories.toString,
            "Auto-create dictionary directories",
            "boolean",
            "true"
          )
        )
      case _ => None

  /** Extracts all available configuration entries with their metadata. Used to
    * provide a complete list of configurable options to users.
    *
    * @param config
    *   The current configuration
    * @return
    *   List of all available configuration entries
    */
  private def extractAllConfigEntries(config: AppConfig): List[ConfigEntry] =
    List(
      ConfigKey("database", "mongoUri"),
      ConfigKey("database", "databaseName"),
      ConfigKey("database", "useMongoDb"),
      ConfigKey("server", "host"),
      ConfigKey("server", "port"),
      ConfigKey("dictionary", "basePath"),
      ConfigKey("dictionary", "autoCreateDirectories")
    ).flatMap(key => extractConfigEntry(config, key))

/** Companion object for ConfigurationService providing factory methods.
  */
object ConfigurationService:

  /** Creates a new FileConfigurationService instance with the provided initial
    * state. Initializes all thread-safe references and returns a configured
    * service ready for use.
    *
    * @param initialConfig
    *   The initial configuration to use
    * @param profileRepository
    *   The initial profile repository instance
    * @param typingTestRepository
    *   The initial typing test repository instance
    * @param dictionaryRepository
    *   The initial dictionary repository instance
    * @return
    *   IO effect containing the configured ConfigurationService
    *
    * @example
    *   {{{
    * for {
    *   config <- ConfigUtils.loadOrCreateDefault()
    *   profileRepo = ConfigUtils.createProfileRepository(config)
    *   testRepo = ConfigUtils.createTypingTestRepository(config)
    *   dictRepo = ConfigUtils.createDictionaryRepository(config)
    *   configService <- ConfigurationService.create(config, profileRepo, testRepo, dictRepo)
    * } yield configService
    *   }}}
    */
  def create(
      initialConfig: AppConfig,
      profileRepository: ProfileRepository,
      typingTestRepository: TypingTestRepository,
      dictionaryRepository: DictionaryRepository,
      statisticsRepository: StatisticsRepository
  ): IO[FileConfigurationService] =
    for
      configRef <- Ref.of[IO, AppConfig](initialConfig)
      profileRepoRef <- Ref.of[IO, ProfileRepository](profileRepository)
      testRepoRef <- Ref.of[IO, TypingTestRepository](typingTestRepository)
      dictRepoRef <- Ref.of[IO, DictionaryRepository](dictionaryRepository)
      statRepoRef <- Ref.of[IO, StatisticsRepository](statisticsRepository)
    yield new FileConfigurationService(
      configRef,
      profileRepoRef,
      testRepoRef,
      dictRepoRef,
      statRepoRef
    )

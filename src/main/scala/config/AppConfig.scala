package config

import cats.effect.IO
import cats.effect.std.Console
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import typingTest.dictionary.repository.{
  DictionaryRepository,
  FileDictionaryRepository
}
import typingTest.tests.repository.{
  InMemoryTypingTestRepository,
  MongoTypingTestRepository,
  TypingTestRepository
}
import users_management.repository.{
  DatabaseInfos,
  InMemoryProfileRepository,
  MongoProfileRepository,
  ProfileRepository
}

import java.io.File
import java.nio.file.{Files, Paths}
import scala.util.{Failure, Success, Try}

/** Configuration settings for database connectivity and collections. Supports
  * both MongoDB and in-memory storage options.
  *
  * @param mongoUri
  *   MongoDB connection URI with authentication if needed
  * @param databaseName
  *   Name of the MongoDB database to use
  * @param profilesCollection
  *   Collection name for storing user profiles
  * @param testsCollection
  *   Collection name for storing typing test results
  * @param useMongoDb
  *   Whether to use MongoDB (true) or in-memory storage (false)
  *
  * @example
  *   {{{
  * val mongoConfig = DatabaseConfig(
  *   mongoUri = "mongodb://user:pass@localhost:27017",
  *   databaseName = "keyla_prod",
  *   useMongoDb = true
  * )
  *   }}}
  */
case class DatabaseConfig(
    mongoUri: String = "mongodb://localhost:27017",
    databaseName: String = "keyla_db",
    profilesCollection: String = "profiles",
    testsCollection: String = "typing_tests",
    useMongoDb: Boolean = false
)

/** Configuration settings for the HTTP server. Controls where the server binds
  * and CORS behavior. Note: Host and port changes require a server restart to
  * take effect.
  *
  * @param host
  *   The host address to bind the server to (e.g., "localhost", "0.0.0.0")
  * @param port
  *   The port number to bind the server to (1-65535)
  * @param enableCors
  *   Whether to enable Cross-Origin Resource Sharing headers
  *
  * @example
  *   {{{
  * val serverConfig = ServerConfig(
  *   host = "0.0.0.0",  // Listen on all interfaces
  *   port = 9090,
  *   enableCors = true
  * )
  *   }}}
  */
case class ServerConfig(
    host: String = "localhost",
    port: Int = 8080,
    enableCors: Boolean = true
)

/** Configuration settings for dictionary file management. Controls where
  * dictionary files are stored and how they're processed.
  *
  * @param basePath
  *   Base directory path for dictionary files (relative or absolute)
  * @param fileExtension
  *   File extension for dictionary files (including the dot)
  * @param autoCreateDirectories
  *   Whether to automatically create dictionary directories if they don't exist
  *
  * @example
  *   {{{
  * val dictConfig = DictionaryConfig(
  *   basePath = "/opt/keyla/dictionaries",
  *   fileExtension = ".txt",
  *   autoCreateDirectories = true
  * )
  *   }}}
  */
case class DictionaryConfig(
    basePath: String = "src/main/resources/dictionaries",
    fileExtension: String = ".txt",
    autoCreateDirectories: Boolean = true
)

/** Main application configuration containing all configuration sections. This
  * is the root configuration object that gets serialized to/from JSON.
  *
  * @param database
  *   Database connectivity and collection settings
  * @param server
  *   HTTP server binding and CORS settings
  * @param dictionary
  *   Dictionary file management settings
  * @param version
  *   Application configuration version for compatibility tracking
  *
  * @example
  *   {{{
  * val config = AppConfig(
  *   database = DatabaseConfig(useMongoDb = true),
  *   server = ServerConfig(port = 9090),
  *   dictionary = DictionaryConfig(basePath = "/custom/path")
  * )
  *   }}}
  */
case class AppConfig(
    database: DatabaseConfig = DatabaseConfig(),
    server: ServerConfig = ServerConfig(),
    dictionary: DictionaryConfig = DictionaryConfig(),
    version: String = "1.0.0"
)

/** Companion object for AppConfig providing JSON serialization, file
  * operations, and repository factory methods.
  *
  * This object handles:
  *   - Loading and saving configuration from/to JSON files
  *   - Environment variable configuration
  *   - Repository creation based on configuration settings
  *   - Default configuration generation
  */
object AppConfig:
  // JSON codecs for serialization/deserialization
  given JsonValueCodec[DatabaseConfig] = JsonCodecMaker.make
  given JsonValueCodec[ServerConfig] = JsonCodecMaker.make
  given JsonValueCodec[DictionaryConfig] = JsonCodecMaker.make
  given JsonValueCodec[AppConfig] = JsonCodecMaker.make

  private val CONFIG_FILENAME = "keyla-config.json"

  /** Gets the full path to the configuration file. The config file is always
    * stored in the current working directory.
    *
    * @return
    *   Absolute path to the configuration file
    */
  def getConfigPath: String =
    val userDir = System.getProperty("user.dir")
    s"$userDir/$CONFIG_FILENAME"

  /** Loads configuration from file or creates a default configuration if the
    * file doesn't exist. This is the main entry point for application
    * configuration loading.
    *
    * @return
    *   IO effect containing the loaded or default configuration
    *
    * @example
    *   {{{
    * for {
    *   config <- AppConfig.loadOrCreateDefault()
    *   _ <- IO.println(s"Using port: ${config.server.port}")
    * } yield config
    *   }}}
    */
  def loadOrCreateDefault(): IO[AppConfig] =
    for
      configPath <- IO.pure(getConfigPath)
      config <- loadFromFile(configPath).flatMap {
        case Some(config) =>
          Console[IO].println(s"Loaded configuration from: $configPath") *> IO
            .pure(config)
        case None =>
          val defaultConfig = AppConfig()
          saveToFile(defaultConfig, configPath) *>
            Console[IO].println(
              s"Created default configuration at: $configPath"
            ) *>
            IO.pure(defaultConfig)
      }
    yield config

  /** Loads configuration from a specific file path. Returns None if the file
    * doesn't exist or cannot be parsed.
    *
    * @param filePath
    *   Path to the configuration file
    * @return
    *   IO effect containing Some(config) if successful, None if file not found
    *   or invalid
    */
  def loadFromFile(filePath: String): IO[Option[AppConfig]] =
    IO {
      val file = new File(filePath)
      if file.exists() && file.canRead then
        Try {
          val content = Files.readString(Paths.get(filePath))
          readFromString[AppConfig](content)
        } match
          case Success(config) => Some(config)
          case Failure(ex) =>
            println(s"Failed to parse config file: ${ex.getMessage}")
            None
      else None
    }

  /** Saves configuration to a specific file path in pretty-printed JSON format.
    * Creates parent directories if they don't exist.
    *
    * @param config
    *   The configuration to save
    * @param filePath
    *   Path where to save the configuration file
    * @return
    *   IO effect that completes when the file is saved
    * @throws Exception
    *   if the file cannot be written
    */
  def saveToFile(config: AppConfig, filePath: String): IO[Unit] =
    IO {
      try
        val jsonContent =
          writeToString(config, WriterConfig.withIndentionStep(2))
        Files.write(Paths.get(filePath), jsonContent.getBytes)
      catch
        case ex: Exception =>
          println(s"Failed to save config file: ${ex.getMessage}")
          throw ex
    }

  /** Creates configuration from environment variables. Useful for containerized
    * deployments where configuration is provided via environment.
    *
    * Environment variable mappings:
    *   - MONGO_URI -> database.mongoUri
    *   - DB_NAME -> database.databaseName
    *   - PROFILES_COLLECTION -> database.profilesCollection
    *   - TESTS_COLLECTION -> database.testsCollection
    *   - USE_MONGODB -> database.useMongoDb
    *   - SERVER_HOST -> server.host
    *   - SERVER_PORT -> server.port
    *   - ENABLE_CORS -> server.enableCors
    *   - DICT_BASE_PATH -> dictionary.basePath
    *   - DICT_FILE_EXT -> dictionary.fileExtension
    *   - DICT_AUTO_CREATE_DIRS -> dictionary.autoCreateDirectories
    *
    * @return
    *   AppConfig with values from environment variables or defaults
    *
    * @example
    *   {{{
    * // Set environment variables:
    * // export SERVER_PORT=9090
    * // export USE_MONGODB=true
    * val config = AppConfig.fromEnvironment
    *   }}}
    */
  def fromEnvironment: AppConfig = AppConfig(
    database = DatabaseConfig(
      mongoUri = sys.env.getOrElse("MONGO_URI", "mongodb://localhost:27017"),
      databaseName = sys.env.getOrElse("DB_NAME", "keyla_db"),
      profilesCollection = sys.env.getOrElse("PROFILES_COLLECTION", "profiles"),
      testsCollection = sys.env.getOrElse("TESTS_COLLECTION", "typing_tests"),
      useMongoDb =
        sys.env.getOrElse("USE_MONGODB", "true").toLowerCase == "true"
    ),
    server = ServerConfig(
      host = sys.env.getOrElse("SERVER_HOST", "localhost"),
      port =
        sys.env.getOrElse("SERVER_PORT", "8080").toIntOption.getOrElse(8080),
      enableCors =
        sys.env.getOrElse("ENABLE_CORS", "true").toLowerCase == "true"
    ),
    dictionary = DictionaryConfig(
      basePath =
        sys.env.getOrElse("DICT_BASE_PATH", "src/main/resources/dictionaries"),
      fileExtension = sys.env.getOrElse("DICT_FILE_EXT", ".txt"),
      autoCreateDirectories =
        sys.env.getOrElse("DICT_AUTO_CREATE_DIRS", "true").toLowerCase == "true"
    )
  )

  /** Creates a profile repository instance based on the configuration. Returns
    * either a MongoDB-backed or in-memory repository.
    *
    * @param config
    *   The application configuration
    * @return
    *   ProfileRepository instance (MongoProfileRepository or
    *   InMemoryProfileRepository)
    */
  def createProfileRepository(config: AppConfig): ProfileRepository =
    if config.database.useMongoDb then
      val dbInfos = DatabaseInfos(
        collectionName = config.database.profilesCollection,
        mongoUri = config.database.mongoUri,
        databaseName = config.database.databaseName
      )
      MongoProfileRepository(dbInfos)
    else InMemoryProfileRepository()

  /** Creates a typing test repository instance based on the configuration.
    * Returns either a MongoDB-backed or in-memory repository.
    *
    * @param config
    *   The application configuration
    * @return
    *   TypingTestRepository instance (MongoTypingTestRepository or
    *   InMemoryTypingTestRepository)
    */
  def createTypingTestRepository(config: AppConfig): TypingTestRepository =
    if config.database.useMongoDb then
      val dbInfos = DatabaseInfos(
        collectionName = config.database.testsCollection,
        mongoUri = config.database.mongoUri,
        databaseName = config.database.databaseName
      )
      MongoTypingTestRepository(dbInfos)
    else InMemoryTypingTestRepository()

  /** Creates a dictionary repository instance based on the configuration.
    * Always returns a file-based repository using the configured path and
    * extension.
    *
    * @param config
    *   The application configuration
    * @return
    *   FileDictionaryRepository instance configured with the specified path and
    *   extension
    */
  def createDictionaryRepository(config: AppConfig): DictionaryRepository =
    FileDictionaryRepository(
      config.dictionary.basePath,
      config.dictionary.fileExtension
    )

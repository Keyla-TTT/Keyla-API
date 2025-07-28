package config

import analytics.repository.{
  InMemoryStatisticsRepository,
  MongoStatisticsRepository,
  StatisticsRepository
}
import cats.effect.IO
import cats.effect.std.Console
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*
import common.DatabaseInfos
import typingTest.dictionary.repository.{
  CachedRepository,
  DictionaryRepository,
  FileDictionaryRepository
}
import typingTest.tests.repository.{
  InMemoryTypingTestRepository,
  MongoTypingTestRepository,
  TypingTestRepository
}
import users_management.repository.{
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
    mongoUri: String,
    databaseName: String,
    useMongoDb: Boolean = false
)

/** Configuration settings for the HTTP server. Controls where the server binds.
  * Note: Host and port changes require a server restart to take effect.
  *
  * @param host
  *   The host address to bind the server to (e.g., "localhost", "0.0.0.0")
  * @param port
  *   The port number to bind the server to (1-65535)
  * @param threadPool
  *   Thread pool configuration for optimal production performance
  *
  * @example
  *   {{{
  * val serverConfig = ServerConfig(
  *   host = "0.0.0.0",  // Listen on all interfaces
  *   port = 9090,
  *   threadPool = ThreadPoolConfig(
  *     coreSize = 16,
  *     maxSize = 32,
  *     keepAliveSeconds = 60
  *   )
  * )
  *   }}}
  */
case class ServerConfig(
    host: String = "localhost",
    port: Int = 8080,
    threadPool: ThreadPoolConfig
)

/** Configuration for the server's thread pool to optimize performance and
  * resource usage in production environments.
  *
  * @param coreSize
  *   Number of core threads to maintain in the pool (default: CPU cores)
  * @param maxSize
  *   Maximum number of threads in the pool (default: 2x CPU cores)
  * @param keepAliveSeconds
  *   How long to keep idle threads alive before terminating them
  * @param queueSize
  *   Size of the work queue for pending tasks (0 = unbounded)
  * @param threadNamePrefix
  *   Prefix for thread names for easier debugging and monitoring
  *
  * @example
  *   {{{
  * val threadConfig = ThreadPoolConfig(
  *   coreSize = 8,
  *   maxSize = 16,
  *   keepAliveSeconds = 30,
  *   queueSize = 1000,
  *   threadNamePrefix = "keyla-api"
  * )
  *   }}}
  */
case class ThreadPoolConfig(
    coreSize: Int,
    maxSize: Int,
    keepAliveSeconds: Int,
    queueSize: Int,
    threadNamePrefix: String
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
    basePath: String,
    autoCreateDirectories: Boolean
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
    database: DatabaseConfig,
    server: ServerConfig,
    dictionary: DictionaryConfig,
    version: String
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
object ConfigUtils:
  given JsonValueCodec[DatabaseConfig] = JsonCodecMaker.make
  given JsonValueCodec[ServerConfig] = JsonCodecMaker.make
  given JsonValueCodec[DictionaryConfig] = JsonCodecMaker.make
  given JsonValueCodec[ThreadPoolConfig] = JsonCodecMaker.make
  given JsonValueCodec[AppConfig] = JsonCodecMaker.make

  private val CONFIG_FILENAME = "keyla-config.json"
  private val APP_NAME = "keyla-api"

  /** Gets the appropriate configuration directory based on the environment.
    * Uses standard Linux application data directories:
    *   - System-wide: /var/lib/keyla-api/ (if running as root)
    *   - User-specific: ~/.local/share/keyla-api/ (recommended)
    *   - Fallback: current working directory
    *
    * @return
    *   Absolute path to the configuration directory
    */
  private def getConfigDirectory: String =
    val isRoot = System.getProperty("user.name") == "root"
    val homeDir = System.getProperty("user.home")

    if isRoot then s"/var/lib/$APP_NAME"
    else s"$homeDir/.local/share/$APP_NAME"

  /** Gets the full path to the configuration file. The config file is stored in
    * the standard application data directory for the current user.
    *
    * @return
    *   Absolute path to the configuration file
    */
  def getConfigPath: String =
    val configDir = getConfigDirectory
    s"$configDir/$CONFIG_FILENAME"

  /** Ensures the configuration directory exists, creating it if necessary. This
    * is called before saving configuration files.
    *
    * @return
    *   IO effect that completes when the directory is ready
    */
  private def ensureConfigDirectory(): IO[Unit] =
    IO {
      val configDir = new File(getConfigDirectory)
      if !configDir.exists() then configDir.mkdirs()
    }

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
      configDir <- IO.pure(getConfigDirectory)
      _ <- Console[IO].println(s"Configuration directory: $configDir")
      config <- loadFromFile(configPath).flatMap {
        case Some(config) =>
          Console[IO].println(s"Loaded configuration from: $configPath") *> IO
            .pure(config)
        case None =>
          val defaultConfig = getDefaultConfig
          for
            _ <- saveToFile(defaultConfig, configPath)
            _ <- Console[IO].println(
              s"Created default configuration at: $configPath"
            )
          yield defaultConfig
      }
    yield config

  def getDefaultConfig: AppConfig =
    AppConfig(
      database = DatabaseConfig(
        mongoUri = "mongodb://localhost:27017",
        databaseName = "keyla_db",
        useMongoDb = false
      ),
      server = ServerConfig(
        host = "localhost",
        port = 8080,
        threadPool = ThreadPoolConfig(
          coreSize = Runtime.getRuntime.availableProcessors(),
          maxSize = Runtime.getRuntime.availableProcessors() * 2,
          keepAliveSeconds = 60,
          queueSize = 0,
          threadNamePrefix = "keyla-api"
        )
      ),
      dictionary = DictionaryConfig(
        basePath = "src/main/resources/dictionaries",
        autoCreateDirectories = true
      ),
      version = "1.0.0"
    )

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
          println(s"Reading configuration from: $filePath")
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
    for
      _ <- ensureConfigDirectory()
      _ <- IO {
        try
          println(s"Saving ${config} configuration to: $filePath")
          val jsonContent =
            writeToString(config, WriterConfig.withIndentionStep(2))
          println(s"JSON content: $jsonContent")
          Files.write(Paths.get(filePath), jsonContent.getBytes)
        catch
          case ex: Exception =>
            println(s"Failed to save config file: ${ex.getMessage}")
            throw ex
      }
    yield ()

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
        collectionName = "profiles", // Hardcoded collection name
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
        collectionName = "typing_tests", // Hardcoded collection name
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
    println(
      s"Creating dictionary repository with base path: ${config.dictionary.basePath}"
    )
    CachedRepository(FileDictionaryRepository(config.dictionary.basePath))

  def createStatisticsRepository(config: AppConfig): StatisticsRepository =
    if config.database.useMongoDb then
      val dbInfos = DatabaseInfos(
        collectionName = "statistics",
        mongoUri = config.database.mongoUri,
        databaseName = config.database.databaseName
      )
      MongoStatisticsRepository(dbInfos)
    else InMemoryStatisticsRepository()

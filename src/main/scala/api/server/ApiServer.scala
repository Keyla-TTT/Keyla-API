package api.server

import api.controllers.{
  AnalyticsController,
  ConfigurationController,
  TypingTestController
}
import api.endpoints.ApiEndpoints
import api.routes.ApiRoutes
import cats.effect.{ExitCode, IO, Resource}
import cats.syntax.all.*
import org.http4s.HttpApp
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.Router
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger
import sttp.tapir.server.http4s.Http4sServerInterpreter
import sttp.tapir.swagger.bundle.SwaggerInterpreter

import scala.concurrent.ExecutionContext

/** HTTP server implementation for the Keyla Typing Test API using Http4s and
  * Blaze.
  *
  * This class provides a complete HTTP server setup with:
  *   - REST API endpoints for typing test functionality
  *   - Automatic Swagger/OpenAPI documentation generation
  *   - CORS support and proper error handling
  *   - Configurable host and port binding
  *   - Resource management for graceful server lifecycle
  *
  * =Architecture=
  *
  * The server follows a layered architecture:
  * {{{
  * HTTP Client Requests
  *        ↓
  * ApiServer (Http4s/Blaze)
  *        ↓
  * ApiRoutes (Tapir route interpretation)
  *        ↓
  * TypingTestController (request handling)
  *        ↓
  * TypingTestService (business logic)
  *        ↓
  * Repositories (data persistence)
  * }}}
  *
  * =Features=
  *
  *   - '''REST API''': Complete RESTful API for typing test management
  *   - '''Swagger Documentation''': Auto-generated OpenAPI 3.0 documentation at
  *     `/docs`
  *   - '''Type Safety''': Tapir-based endpoint definitions with compile-time
  *     validation
  *   - '''Resource Management''': Proper resource cleanup and graceful shutdown
  *   - '''Configurable Binding''': Support for custom host and port
  *     configuration
  *   - '''Production Ready''': Structured logging and error handling
  *
  * =Available Endpoints=
  *
  * ==Profile Management==
  *   - `POST /api/profiles` - Create user profile
  *   - `GET /api/profiles` - List all profiles
  *
  * ==Typing Tests==
  *   - `POST /api/tests` - Request new typing test
  *   - `GET /api/tests/{testId}` - Get test by ID
  *   - `GET /api/profiles/{profileId}/tests` - Get tests by profile
  *   - `GET /api/tests/language/{language}` - Get tests by language
  *   - `GET /api/profiles/{profileId}/last-test` - Get last incomplete test
  *   - `PUT /api/tests/{testId}/results` - Submit test results
  *
  * ==Dictionaries==
  *   - `GET /api/dictionaries` - List all dictionaries
  *   - `GET /api/languages` - List all languages
  *   - `GET /api/languages/{language}/dictionaries` - Get dictionaries by
  *     language
  *
  * ==Configuration Management==
  *   - `GET /api/config` - List all configuration entries
  *   - `GET /api/config/{section}/{key}` - Get specific config entry
  *   - `PUT /api/config` - Update configuration
  *   - `GET /api/config/current` - Get complete current config
  *   - `POST /api/config/reload` - Reload config from file
  *   - `POST /api/config/reset` - Reset to default configuration
  *
  * ==Documentation==
  *   - `GET /docs` - Swagger UI for API documentation
  *   - `GET /docs/docs.yaml` - OpenAPI specification
  *
  * @param controller
  *   The typing test controller that handles business logic
  *
  * @example
  *   {{{
  * // Create and start server with default settings
  * val server = ApiServer(typingTestController)
  * server.serve // Starts on localhost:8080
  *
  * // Start server with custom host and port
  * server.serveOn(9090, "0.0.0.0") // All interfaces, port 9090
  *
  * // Use as a resource for proper lifecycle management
  * server.resource(8080, "localhost").use { server =>
  *   IO.println(s"Server started on ${server.address}") *>
  *   IO.never // Keep server running
  * }
  *   }}}
  */
class ApiServer(
    configurationController: ConfigurationController,
    typingTestController: TypingTestController,
    analyticsController: AnalyticsController
):

  /** Logger instance for structured logging throughout the server lifecycle.
    * Uses SLF4J backend for integration with standard logging frameworks.
    */
  given Logger[IO] = Slf4jLogger.getLogger[IO]

  /** API routes handler that interprets Tapir endpoints into Http4s routes.
    * Connects the controller business logic to HTTP request/response handling.
    */
  private val apiRoutes = ApiRoutes(
    configurationController,
    typingTestController,
    analyticsController
  )

  /** Swagger/OpenAPI documentation endpoints generated from the API
    * definitions. Provides interactive API documentation and machine-readable
    * OpenAPI specs.
    */
  private val swaggerEndpoints = SwaggerInterpreter()
    .fromEndpoints[IO](ApiEndpoints.getAllEndpoints, "Typing Test API", "1.0.0")

  /** Http4s routes for serving the Swagger documentation. Includes both the
    * interactive Swagger UI and raw OpenAPI specification.
    */
  private val swaggerRoutes =
    Http4sServerInterpreter[IO]().toRoutes(swaggerEndpoints)

  /** Complete HTTP application combining API routes and documentation. Uses
    * Http4s Router to mount both API endpoints and Swagger docs at the root
    * path.
    */
  private val httpApp: HttpApp[IO] = Router(
    "/" -> (Http4sServerInterpreter[IO]().toRoutes(
      apiRoutes.routes
    ) <+> swaggerRoutes)
  ).orNotFound

  /** Execution context for the HTTP server operations. Uses the global
    * execution context for simplicity - in production environments consider
    * using a dedicated thread pool.
    */
  implicit val ec: ExecutionContext =
    scala.concurrent.ExecutionContext.Implicits.global

  /** Starts the HTTP server with default settings (localhost:8080). This is a
    * fire-and-forget operation that runs the server indefinitely.
    *
    * The server will:
    *   1. Bind to localhost on port 8080
    *   2. Start accepting HTTP requests
    *   3. Run indefinitely until the JVM terminates
    *   4. Return ExitCode.Success when shutdown
    *
    * @return
    *   IO effect that starts the server and completes with ExitCode.Success
    *
    * @example
    *   {{{
    * val server = ApiServer(controller)
    *
    * // Start server - this will block until JVM shutdown
    * server.serve.unsafeRunSync()
    *   }}}
    */
  def serve: IO[ExitCode] = BlazeServerBuilder[IO]
    .withExecutionContext(ec)
    .bindHttp(8080, "localhost")
    .withHttpApp(httpApp)
    .resource
    .use(_ => IO.never)
    .as(ExitCode.Success)

  /** Starts the HTTP server with custom host and port settings. This allows
    * binding to specific network interfaces and ports for different
    * environments.
    *
    * @param port
    *   The port number to bind to (1-65535)
    * @param host
    *   The host address to bind to (e.g., "localhost", "0.0.0.0", specific IP)
    * @return
    *   IO effect that starts the server and completes with ExitCode.Success
    *
    * @example
    *   {{{
    * val server = ApiServer(controller)
    *
    * // Start on all interfaces, port 9090
    * server.serveOn(9090, "0.0.0.0").unsafeRunSync()
    *
    * // Start on specific IP address
    * server.serveOn(8080, "192.168.1.100").unsafeRunSync()
    *   }}}
    */
  def serveOn(port: Int, host: String): IO[ExitCode] = BlazeServerBuilder[IO]
    .withExecutionContext(ec)
    .bindHttp(port, host)
    .withHttpApp(httpApp)
    .resource
    .use(_ => IO.never)
    .as(ExitCode.Success)

  /** Creates a managed resource for the HTTP server with configurable host and
    * port. This is the recommended approach for production applications as it
    * provides proper resource management and graceful shutdown capabilities.
    *
    * The returned Resource will:
    *   1. Start the server when acquired
    *   2. Provide access to the running server instance
    *   3. Gracefully shutdown the server when released
    *   4. Handle cleanup of all associated resources
    *
    * @param port
    *   The port number to bind to (default: 8080)
    * @param host
    *   The host address to bind to (default: "localhost")
    * @return
    *   Resource[IO, Server] that manages the server lifecycle
    *
    * @example
    *   {{{
    * val server = ApiServer(controller)
    *
    * // Use resource for automatic cleanup
    * server.resource().use { runningServer =>
    *   for {
    *     _ <- IO.println(s"Server started on ${runningServer.address}")
    *     _ <- IO.println("Press Ctrl+C to stop...")
    *     _ <- IO.never // Keep running until interrupted
    *   } yield ()
    * }
    *
    * // Production setup with custom configuration
    * server.resource(port = 9090, host = "0.0.0.0").use { server =>
    *   // Server automatically shuts down when this block exits
    *   applicationLogic(server)
    * }
    *   }}}
    */
  def resource(
      port: Int = 8080,
      host: String = "localhost"
  ): Resource[IO, org.http4s.server.Server] =
    BlazeServerBuilder[IO]
      .withExecutionContext(ec)
      .bindHttp(port, host)
      .withHttpApp(httpApp)
      .resource

/** Companion object for ApiServer providing factory methods and utilities.
  */
object ApiServer:

  /** Creates a new ApiServer instance with the provided controller. This is the
    * primary factory method for creating server instances.
    *
    * @param controller
    *   The typing test controller that provides business logic
    * @return
    *   A configured ApiServer ready to start
    *
    * @example
    *   {{{
    * // Create server instance
    * val controller = // ... create controller with services and repositories
    * val server = ApiServer(controller)
    *
    * // Start server
    * server.serve.unsafeRunSync()
    *   }}}
    */
  def apply(
      configurationController: ConfigurationController,
      typingTestController: TypingTestController,
      analyticsController: AnalyticsController
  ): ApiServer =
    new ApiServer(
      configurationController,
      typingTestController,
      analyticsController
    )

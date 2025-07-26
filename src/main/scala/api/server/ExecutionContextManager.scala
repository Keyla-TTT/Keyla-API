package api.server

import cats.effect.{IO, Resource}
import config.ThreadPoolConfig
import org.typelevel.log4cats.Logger
import org.typelevel.log4cats.slf4j.Slf4jLogger

import java.util.concurrent.{
  Executors,
  ThreadFactory,
  ThreadPoolExecutor,
  TimeUnit
}
import scala.concurrent.ExecutionContext

/** Manages execution contexts with proper resource lifecycle for production
  * environments. Provides thread pools optimized for HTTP server workloads with
  * automatic cleanup and monitoring capabilities.
  *
  * This class ensures:
  *   - Proper thread pool sizing based on CPU cores and configuration
  *   - Graceful shutdown with timeout handling
  *   - Resource cleanup to prevent memory leaks
  *   - Thread naming for easier debugging and monitoring
  *   - Configurable queue sizes and keep-alive settings
  *
  * @example
  *   {{{
  * val threadConfig = ThreadPoolConfig(
  *   coreSize = 8,
  *   maxSize = 16,
  *   keepAliveSeconds = 30
  * )
  *
  * ExecutionContextManager.create(threadConfig).use { ec =>
  *   // Use the execution context for server operations
  *   BlazeServerBuilder[IO]
  *     .withExecutionContext(ec)
  *     .bindHttp(8080, "localhost")
  *     .resource
  * }
  *   }}}
  */
object ExecutionContextManager:

  given Logger[IO] = Slf4jLogger.getLogger[IO]

  /** Creates a managed execution context resource with the specified thread
    * pool configuration. The returned Resource will automatically handle thread
    * pool lifecycle including proper shutdown.
    *
    * @param config
    *   Thread pool configuration specifying size, queue, and naming
    * @return
    *   Resource[IO, ExecutionContext] that manages the thread pool lifecycle
    */
  def create(config: ThreadPoolConfig): Resource[IO, ExecutionContext] =
    Resource.make(createExecutionContext(config))(shutdownExecutionContext)

  /** Creates an execution context with the specified thread pool configuration.
    * This method sets up a ThreadPoolExecutor optimized for HTTP server
    * workloads.
    *
    * @param config
    *   Thread pool configuration
    * @return
    *   IO effect that creates the execution context
    */
  private def createExecutionContext(
      config: ThreadPoolConfig
  ): IO[ExecutionContext] =
    for
      _ <- Logger[IO].info(
        s"Creating execution context with coreSize=${config.coreSize}, " +
          s"maxSize=${config.maxSize}, keepAlive=${config.keepAliveSeconds}s"
      )
      threadFactory = createThreadFactory(config.threadNamePrefix)
      executor = createThreadPoolExecutor(config, threadFactory)
      ec = ExecutionContext.fromExecutor(executor)
      _ <- Logger[IO].info("Execution context created successfully")
    yield ec

  /** Creates a thread factory with custom naming for better debugging and
    * monitoring capabilities.
    *
    * @param namePrefix
    *   Prefix for thread names
    * @return
    *   ThreadFactory that creates named threads
    */
  private def createThreadFactory(namePrefix: String): ThreadFactory =
    new ThreadFactory:
      private val counter = new java.util.concurrent.atomic.AtomicInteger(1)

      override def newThread(r: Runnable): Thread =
        val thread = new Thread(r, s"$namePrefix-${counter.getAndIncrement()}")
        thread.setDaemon(false)
        thread.setPriority(Thread.NORM_PRIORITY)
        thread

  /** Creates a ThreadPoolExecutor with the specified configuration. Uses
    * optimal settings for HTTP server workloads.
    *
    * @param config
    *   Thread pool configuration
    * @param threadFactory
    *   Factory for creating named threads
    * @return
    *   Configured ThreadPoolExecutor
    */
  private def createThreadPoolExecutor(
      config: ThreadPoolConfig,
      threadFactory: ThreadFactory
  ): ThreadPoolExecutor =
    val queue =
      if config.queueSize > 0 then
        new java.util.concurrent.LinkedBlockingQueue[Runnable](config.queueSize)
      else new java.util.concurrent.LinkedBlockingQueue[Runnable]()

    new ThreadPoolExecutor(
      config.coreSize,
      config.maxSize,
      config.keepAliveSeconds,
      TimeUnit.SECONDS,
      queue,
      threadFactory,
      new ThreadPoolExecutor.CallerRunsPolicy()
    )

  /** Gracefully shuts down the execution context with proper timeout handling.
    * Attempts to shutdown gracefully first, then forces shutdown if necessary.
    *
    * @param ec
    *   The execution context to shutdown
    * @return
    *   IO effect that completes when shutdown is finished
    */
  private def shutdownExecutionContext(ec: ExecutionContext): IO[Unit] =
    ec match
      case ec: ExecutionContext with scala.concurrent.ExecutionContextExecutor =>
        val executor =
          ec.asInstanceOf[scala.concurrent.ExecutionContextExecutor]
        executor match
          case tp: ThreadPoolExecutor =>
            shutdownThreadPoolExecutor(tp)
          case _ =>
            Logger[IO].warn(
              "ExecutionContext is not a ThreadPoolExecutor, skipping shutdown"
            )
      case _ =>
        Logger[IO].warn(
          "ExecutionContext is not an ExecutionContextExecutor, skipping shutdown"
        )

  /** Shuts down a ThreadPoolExecutor with graceful timeout handling.
    *
    * @param executor
    *   The ThreadPoolExecutor to shutdown
    * @return
    *   IO effect that completes when shutdown is finished
    */
  private def shutdownThreadPoolExecutor(
      executor: ThreadPoolExecutor
  ): IO[Unit] =
    for
      _ <- Logger[IO].info("Initiating thread pool shutdown")
      _ <- IO.blocking(executor.shutdown())
      _ <- IO.blocking {
        if !executor.awaitTermination(30, TimeUnit.SECONDS) then
          Logger[IO].warn(
            "Thread pool did not shutdown gracefully, forcing shutdown"
          )
          executor.shutdownNow()
          if !executor.awaitTermination(10, TimeUnit.SECONDS) then
            Logger[IO].error("Thread pool could not be shutdown")
      }
      _ <- Logger[IO].info("Thread pool shutdown completed")
    yield ()

package analytics.repository

import analytics.model.Statistics
import common.Repository

/** Repository trait for managing typing test statistics.
  *
  * This trait defines the contract for storing and retrieving Statistics
  * entities. Implementations of this trait should handle the persistence layer
  * operations for typing test statistics (e.g. database access).
  *
  * =Statistics Management=
  *
  * The repository provides operations for:
  *   - Storing individual test statistics
  *   - Retrieving statistics by test ID or user ID
  *   - Listing all statistics for a user
  *   - Cleaning up old or unwanted statistics
  *
  * =Usage=
  *
  * This trait is typically implemented by concrete classes that handle
  * statistics persistence, such as `MongoStatisticsRepository` for MongoDB
  * storage or `InMemoryStatisticsRepository` for testing.
  *
  * @example
  *   {{{
  * val repository: StatisticsRepository = MongoStatisticsRepository()
  * val stats = TestStatistics("test-123", "user-456", 75.0, 95.0, List(1, 3), 1000L)
  * val saved = repository.save(stats)
  * val retrieved = repository.get("test-123")
  * val userStats = repository.list("user-456")
  *   }}}
  */
trait StatisticsRepository extends Repository:

  /** Retrieves statistics by test ID.
    *
    * @param testId
    *   The unique identifier of the test
    * @return
    *   Some(Statistics) if found, None if not found
    */
  def get(testId: String): Option[Statistics]

  /** Saves statistics to the repository.
    *
    * @param statistics
    *   The statistics to save
    * @return
    *   The saved statistics with any system-generated fields populated
    */
  def save(statistics: Statistics): Statistics

  /** Deletes all statistics for a specific user.
    *
    * @param userId
    *   The unique identifier of the user
    * @return
    *   true if any statistics were deleted, false if none found
    */
  def deleteAll(userId: String): Boolean

  /** Lists all statistics for a specific user.
    *
    * @param userId
    *   The unique identifier of the user
    * @return
    *   A list containing all statistics for the user
    */
  def list(userId: String): List[Statistics]

  /** Cleans up the statistics repository.
    *
    * This method may remove old statistics, compact storage, or perform other
    * maintenance operations depending on the implementation.
    *
    * @return
    *   true if cleanup was successful, false otherwise
    */
  def clean(): Boolean

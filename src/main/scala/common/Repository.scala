package common

/** Base trait for all repository implementations in the application.
  *
  * This trait defines the common contract that all repository implementations
  * must follow. It provides a standardized interface for resource management
  * and cleanup operations across different data storage backends.
  *
  * =Repository Pattern=
  *
  * The repository pattern abstracts data access logic and provides a consistent
  * interface for data operations regardless of the underlying storage mechanism
  * (database, file system, in-memory, etc.).
  *
  * =Resource Management=
  *
  * All repositories should implement proper resource cleanup to prevent memory
  * leaks and ensure efficient resource utilization.
  *
  * =Usage=
  *
  * This trait is extended by specific repository traits that define
  * domain-specific operations for different entities (profiles, tests,
  * statistics, etc.).
  *
  * @example
  *   {{{
  * trait ProfileRepository extends Repository:
  *   def get(id: String): Option[Profile]
  *   def create(profile: Profile): Profile
  *   // ... other profile-specific operations
  *
  * class MongoProfileRepository extends ProfileRepository:
  *   def close(): Unit = {
  *     // Close MongoDB connections
  *   }
  *   // ... implement other methods
  *   }}}
  */
trait Repository:
  /** Closes the repository and releases all associated resources.
    *
    * This method should be called when the repository is no longer needed to
    * ensure proper cleanup of database connections, file handles, or other
    * system resources.
    *
    * Implementations should:
    *   - Close database connections
    *   - Release file handles
    *   - Clear caches
    *   - Perform any other necessary cleanup
    *
    * @example
    *   {{{
    * val repository = MongoProfileRepository()
    * try
    *   // Use repository
    *   val profile = repository.get("user-123")
    * finally
    *   repository.close() // Ensure cleanup
    *   }}}
    */
  def close(): Unit

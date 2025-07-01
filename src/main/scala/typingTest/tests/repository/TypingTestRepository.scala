package typingTest.tests.repository

import typingTest.tests.model.PersistedTypingTest

/** Repository trait for managing typing tests.
  *
  * This trait defines the contract for storing and retrieving
  * PersistedTypingTest entities. Implementations of this trait should handle
  * the persistence layer operations for typing tests (e.g. database access).
  */
trait TypingTestRepository:

  /** Retrieves a typing test by its ID.
    *
    * @param id
    *   The unique identifier of the test to retrieve
    * @return
    *   Some(PersistedTypingTest) if found, None if not found
    */
  def get(id: String): Option[PersistedTypingTest]

  /** Creates a new typing test in the repository.
    *
    * @param test
    *   The typing test to create
    * @return
    *   The created test with any system-generated fields populated
    */
  def create(test: PersistedTypingTest): PersistedTypingTest

  /** Updates an existing typing test.
    *
    * @param test
    *   The test with updated information
    * @return
    *   Some(PersistedTypingTest) if the test was updated, None if not found
    */
  def update(test: PersistedTypingTest): Option[PersistedTypingTest]

  /** Deletes a typing test by its ID.
    *
    * @param id
    *   The unique identifier of the test to delete
    * @return
    *   true if the test was deleted, false if not found
    */
  def delete(id: String): Boolean

  /** Deletes all typing tests from the repository.
    *
    * @return
    *   true if any tests were deleted, false if repository was empty
    */
  def deleteAll(): Boolean

  /** Lists all typing tests in the repository.
    *
    * @return
    *   A list containing all typing tests
    */
  def list(): List[PersistedTypingTest]

  /** Retrieves all typing tests for a specific profile.
    *
    * @param profileId
    *   The ID of the profile whose tests to retrieve
    * @return
    *   A list of typing tests for the specified profile
    */
  def getByProfileId(profileId: String): List[PersistedTypingTest]

  /** Retrieves typing tests by language.
    *
    * @param language
    *   The language to filter by
    * @return
    *   A list of typing tests in the specified language
    */
  def getByLanguage(language: String): List[PersistedTypingTest]

  /** Retrieves typing tests for a profile filtered by language.
    *
    * @param profileId
    *   The ID of the profile
    * @param language
    *   The language to filter by
    * @return
    *   A list of typing tests for the profile in the specified language
    */
  def getByProfileIdAndLanguage(
      profileId: String,
      language: String
  ): List[PersistedTypingTest]

  /** Retrieves the last non-completed test for a profile.
    *
    * @param profileId
    *   The ID of the profile
    * @return
    *   Some(PersistedTypingTest) if found, None if no non-completed test exists
    */
  def getLastNonCompletedByProfileId(
      profileId: String
  ): Option[PersistedTypingTest]

  /** Deletes all non-completed tests for a profile.
    *
    * @param profileId
    *   The ID of the profile
    * @return
    *   The number of tests deleted
    */
  def deleteNonCompletedByProfileId(profileId: String): Int

  /** Retrieves a completed test by its ID.
    *
    * @param id
    *   The unique identifier of the test
    * @return
    *   Some(PersistedTypingTest) if found and completed, None if not found or
    *   not completed
    */
  def getCompletedById(id: String): Option[PersistedTypingTest]

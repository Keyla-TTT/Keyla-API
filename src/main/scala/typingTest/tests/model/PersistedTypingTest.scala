package typingTest.tests.model

import com.github.nscala_time.time.Imports.DateTime
import typingTest.dictionary.model.Dictionary

/** Represents a typing test that can be persisted in the repository
  * @param id
  *   Unique identifier for the test
  * @param profileId
  *   ID of the profile that requested this test
  * @param testData
  *   The actual typing test data
  * @param createdAt
  *   When the test was created
  * @param language
  *   Language of the test
  * @param wordCount
  *   Number of words in the test
  * @param completedAt
  *   When the test was completed
  * @param accuracy
  *   The accuracy of the test
  * @param rawAccuracy
  *   The raw accuracy of the test
  * @param testTime
  *   The time taken to complete the test
  * @param errorCount
  *   The number of errors in the test
  * @param errorWordIndices
  *   The indices of words that were incorrectly typed
  * @param timeLimit
  *   The time limit for the test
  */
case class PersistedTypingTest(
    id: Option[String],
    profileId: String,
    testData: TypingTest[String] & DefaultContext,
    createdAt: DateTime,
    language: String,
    wordCount: Int,
    completedAt: Option[DateTime] = None,
    accuracy: Option[Double] = None,
    rawAccuracy: Option[Double] = None,
    testTime: Option[Long] = None,
    errorCount: Option[Int] = None,
    errorWordIndices: Option[List[Int]] = None,
    timeLimit: Option[Long] = None
):
  /** Creates a copy of this test with optional field updates
    * @param id
    *   The new id or current if not specified
    * @param profileId
    *   The new profile id or current if not specified
    * @param testData
    *   The new test data or current if not specified
    * @param createdAt
    *   The new creation time or current if not specified
    * @param language
    *   The new language or current if not specified
    * @param wordCount
    *   The new word count or current if not specified
    * @param completedAt
    *   The completion time or current if not specified
    * @param accuracy
    *   The accuracy or current if not specified
    * @param rawAccuracy
    *   The raw accuracy or current if not specified
    * @param testTime
    *   The test time or current if not specified
    * @param errorCount
    *   The error count or current if not specified
    * @param errorWordIndices
    *   The error word indices or current if not specified
    * @param timeLimit
    *   The time limit or current if not specified
    * @return
    *   A new PersistedTypingTest instance with the updated fields
    */
  def copy(
      id: Option[String] = this.id,
      profileId: String = this.profileId,
      testData: TypingTest[String] & DefaultContext = this.testData,
      createdAt: DateTime = this.createdAt,
      language: String = this.language,
      wordCount: Int = this.wordCount,
      completedAt: Option[DateTime] = this.completedAt,
      accuracy: Option[Double] = this.accuracy,
      rawAccuracy: Option[Double] = this.rawAccuracy,
      testTime: Option[Long] = this.testTime,
      errorCount: Option[Int] = this.errorCount,
      errorWordIndices: Option[List[Int]] = this.errorWordIndices,
      timeLimit: Option[Long] = this.timeLimit
  ): PersistedTypingTest = PersistedTypingTest(
    id,
    profileId,
    testData,
    createdAt,
    language,
    wordCount,
    completedAt,
    accuracy,
    rawAccuracy,
    testTime,
    errorCount,
    errorWordIndices,
    timeLimit
  )

  def isCompleted: Boolean = completedAt.isDefined

object PersistedTypingTest:
  /** Creates a new PersistedTypingTest from a TypingTest and request metadata
    * @param profileId
    *   ID of the profile that requested the test
    * @param testData
    *   The typing test data
    * @param language
    *   Language of the test
    * @return
    *   A new PersistedTypingTest instance
    */
  def apply(
      profileId: String,
      testData: TypingTest[String] & DefaultContext,
      language: String
  ): PersistedTypingTest =
    PersistedTypingTest(
      id = None,
      profileId = profileId,
      testData = testData,
      createdAt = DateTime.now(),
      language = language,
      wordCount = testData.words.length
    )

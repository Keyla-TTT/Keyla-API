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
  * @param wordCount
  *   Number of words in the test
  */
case class PersistedTypingTest(
    id: Option[String],
    profileId: String,
    testData: TypingTest[String] & DefaultContext,
    createdAt: DateTime,
    wordCount: Int
)

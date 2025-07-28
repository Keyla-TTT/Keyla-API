package analytics.model

/** Trait representing typing test statistics and performance metrics.
  *
  * This trait defines the structure for storing and accessing typing test
  * performance data including speed (WPM), accuracy, error tracking, and
  * temporal information. It provides a common interface for all statistics
  * implementations.
  *
  * =Key Metrics=
  *
  *   - '''WPM (Words Per Minute)''': Measures typing speed
  *   - '''Accuracy''': Percentage of correctly typed characters/words
  *   - '''Error Tracking''': Detailed error positions and counts
  *   - '''Timestamps''': When the test was performed
  *
  * =Usage=
  *
  * This trait is typically implemented by concrete classes that store
  * statistics data, such as `TestStatistics` for individual test results.
  *
  * @example
  *   {{{
  * val stats: Statistics = TestStatistics(
  *   testId = "test-123",
  *   userId = "user-456",
  *   wpm = 75.5,
  *   accuracy = 95.2,
  *   errors = List(10, 25, 40),
  *   timestamp = System.currentTimeMillis()
  * )
  * println(s"User typed at ${stats.wpm} WPM with ${stats.accuracy}% accuracy")
  *   }}}
  */
trait Statistics:
  /** Unique identifier of the user who took the test
    * @return
    *   String representing the user's unique identifier
    */
  def userId: String

  /** Unique identifier of the typing test
    * @return
    *   String representing the test's unique identifier
    */
  def testId: String

  /** Words per minute (WPM) calculated from the typing speed
    * @return
    *   Double representing the typing speed in words per minute
    */
  def wpm: Double

  /** Accuracy percentage of the typed text compared to the target text
    * @return
    *   Double representing accuracy as a percentage (0.0 to 100.0)
    */
  def accuracy: Double

  /** List of error positions made during the typing test
    * @return
    *   List of integers representing word indices where errors occurred
    */
  def errors: List[Int]

  /** Timestamp of when the statistics were recorded
    * @return
    *   Long representing the timestamp in milliseconds since epoch
    */
  def timestamp: Long

  /** Creates a copy of this statistics object with optional field updates
    *
    * @param testId
    *   The new test ID or current if not specified
    * @param userId
    *   The new user ID or current if not specified
    * @param wpm
    *   The new WPM value or current if not specified
    * @param accuracy
    *   The new accuracy value or current if not specified
    * @param errors
    *   The new errors list or current if not specified
    * @param timestamp
    *   The new timestamp or current if not specified
    * @return
    *   A new Statistics instance with the updated fields
    */
  def copy(
      testId: String = this.testId,
      userId: String = this.userId,
      wpm: Double = this.wpm,
      accuracy: Double = this.accuracy,
      errors: List[Int] = this.errors,
      timestamp: Long = this.timestamp
  ): Statistics

case class TestStatistics(
    testId: String,
    userId: String,
    wpm: Double,
    accuracy: Double,
    errors: List[Int],
    timestamp: Long
) extends Statistics:

  override def copy(
      testId: String = this.testId,
      userId: String = this.userId,
      wpm: Double = this.wpm,
      accuracy: Double = this.accuracy,
      errors: List[Int] = this.errors,
      timestamp: Long = this.timestamp
  ): Statistics =
    TestStatistics(testId, userId, wpm, accuracy, errors, timestamp)

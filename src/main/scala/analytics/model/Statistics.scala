package analytics.model

trait Statistics:
  /** Unique identifier of the user */
  def userId: String

  /** Unique identifier of the test */
  def testId: String

  /** Words per minute (WPM) calculated from the typing speed */
  def wpm: Double

  /** Accuracy percentage of the typed text compared to the target text */
  def accuracy: Double

  /** List of errors made during the typing test */
  def errors: List[Int]

  /** Timestamp of when the statistics were recorded */
  def timestamp: Long

  def copy(
      testId: String = this.testId,
      userId: String = this.userId,
      wpm: Double = this.wpm,
      accuracy: Double = this.accuracy,
      errors: List[Int] = this.errors,
      timestamp: Long = this.timestamp
  ): Statistics

case class UserStatistics(
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
    UserStatistics(testId, userId, wpm, accuracy, errors, timestamp)

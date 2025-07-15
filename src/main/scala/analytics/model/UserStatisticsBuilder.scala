package analytics.model

class UserStatisticsBuilder:
  /** Unique identifier of the user */

  private var testId: String = ""
  private var userId: String = ""
  private var wpm: Double = 0.0
  private var accuracy: Double = 0.0
  private var errors: List[Int] = List.empty
  private var timestamp: Long = System.currentTimeMillis()

  def setUserId(userId: String): UserStatisticsBuilder =
    this.userId = userId
    this

  def setTestId(testId: String): UserStatisticsBuilder =
    this.testId = testId
    this

  def setWpm(wpm: Double): UserStatisticsBuilder =
    this.wpm = wpm
    this

  def setAccuracy(accuracy: Double): UserStatisticsBuilder =
    this.accuracy = accuracy
    this

  def setErrors(errors: List[Int]): UserStatisticsBuilder =
    this.errors = errors
    this

  def setTimestamp(timestamp: Long): UserStatisticsBuilder =
    this.timestamp = timestamp
    this

  def build(): UserStatistics =
    UserStatistics(
      testId = testId,
      userId = userId,
      wpm = wpm,
      accuracy = accuracy,
      errors = errors,
      timestamp = timestamp
    )

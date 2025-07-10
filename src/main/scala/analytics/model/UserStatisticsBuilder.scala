package analytics.model

class UserStatisticsBuilder:
  /** Unique identifier of the user */
  private var date: java.util.Date = new java.util.Date()
  private var userId: String = ""
  private var wpm: Double = 0.0
  private var accuracy: Double = 0.0
  private var timestamp: Long = System.currentTimeMillis()

  def setUserId(userId: String): UserStatisticsBuilder =
    this.userId = userId
    this

  def setDate(date: java.util.Date): UserStatisticsBuilder =
    this.date = date
    this

  def setWpm(wpm: Double): UserStatisticsBuilder =
    this.wpm = wpm
    this

  def setAccuracy(accuracy: Double): UserStatisticsBuilder =
    this.accuracy = accuracy
    this

  def setTimestamp(timestamp: Long): UserStatisticsBuilder =
    this.timestamp = timestamp
    this

  def build(): UserStatistics =
    UserStatistics(userId, date, wpm, accuracy, timestamp)

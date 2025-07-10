package analytics.model

import java.util.Date

trait Statistics:
  /** Unique identifier of the user */
  def userId: String

  /** Words per minute (WPM) calculated from the typing speed */
  def wpm: Double

  /** Accuracy percentage of the typed text compared to the target text */
  def accuracy: Double

  /** Timestamp of when the statistics were recorded */
  def timestamp: Long

case class UserStatistics(
    userId: String,
    date: Date,
    wpm: Double,
    accuracy: Double,
    timestamp: Long
)

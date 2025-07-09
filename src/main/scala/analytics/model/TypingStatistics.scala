package analytics.model

case class TypingStatistics(
    userId: String,
    wpm: Double,
    accuracy: Double,
    timestamp: Long
)

package analytics.factory

import analytics.model.{UserStatistics, UserStatisticsBuilder}

class UserStatisticsFactory:

  def createUserStatistics(
      userId: String,
      date: java.util.Date,
      wpm: Double,
      accuracy: Double,
      timestamp: Long = System.currentTimeMillis()
  ): UserStatistics =
    new UserStatisticsBuilder()
      .setUserId(userId)
      .setDate(date)
      .setWpm(wpm)
      .setAccuracy(accuracy)
      .setTimestamp(timestamp)
      .build()

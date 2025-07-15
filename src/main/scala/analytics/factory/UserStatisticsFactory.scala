package analytics.factory

import analytics.model.{UserStatistics, UserStatisticsBuilder}

class UserStatisticsFactory:

  def createUserStatistics(
      testId: String,
      userId: String,
      wpm: Double,
      accuracy: Double,
      errors: List[Int] = List.empty,
      timestamp: Long = System.currentTimeMillis()
  ): UserStatistics =
    new UserStatisticsBuilder()
      .setTestId(testId)
      .setUserId(userId)
      .setWpm(wpm)
      .setAccuracy(accuracy)
      .setErrors(errors)
      .setTimestamp(timestamp)
      .build()

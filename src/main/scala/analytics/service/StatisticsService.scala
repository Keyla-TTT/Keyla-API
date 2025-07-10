package analytics.service

import analytics.model.{Statistics, UserStatistics}

trait StatisticsService:

  def createUserStatistics(
      userId: String,
      wpm: Double,
      accuracy: Double,
      timestamp: Long = System.currentTimeMillis()
  ): UserStatistics

  def getUserStatistics(userId: String): Option[UserStatistics]

  def deleteAllUserStatistics(userId: String): Boolean

  def listUserStatistics(userId: String): List[UserStatistics]

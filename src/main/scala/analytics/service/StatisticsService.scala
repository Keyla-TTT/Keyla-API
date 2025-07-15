package analytics.service

import analytics.model.{Statistics, UserStatistics}

trait StatisticsService:

  def saveUserStatistics(
      userStatistics: UserStatistics
  ): Statistics

  def getUserStatistics(testId: String): Option[Statistics]

  def deleteAllUserStatistics(userId: String): Boolean

  def listUserStatistics(userId: String): List[Statistics]

  def cleanRepository(): Boolean

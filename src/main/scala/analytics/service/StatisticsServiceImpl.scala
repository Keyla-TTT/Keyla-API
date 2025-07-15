package analytics.service

import analytics.model.{Statistics, UserStatistics}
import analytics.repository.StatisticsRepository

class StatisticsServiceImpl(
    private val statisticsRepository: StatisticsRepository
) extends StatisticsService:

  override def saveUserStatistics(userStatistics: UserStatistics): Statistics =
    statisticsRepository.save(userStatistics)

  override def getUserStatistics(testId: String): Option[Statistics] =
    statisticsRepository.get(testId)

  override def deleteAllUserStatistics(userId: String): Boolean =
    statisticsRepository.deleteAll(userId)

  override def listUserStatistics(userId: String): List[Statistics] =
    statisticsRepository.list(userId)

  override def cleanRepository(): Boolean =
    statisticsRepository.clean()

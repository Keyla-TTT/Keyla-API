package analytics.repository

import analytics.model.Statistics
import org.slf4j.LoggerFactory

import scala.collection.concurrent.TrieMap

class InMemoryStatisticsRepository extends StatisticsRepository:
  private val logger = LoggerFactory.getLogger(getClass)
  private val statisticsStore: TrieMap[String, Statistics] = TrieMap()

  override def get(testId: String): Option[Statistics] =
    require(testId != null && testId.nonEmpty, "TestId cannot be null or empty")
    logger.debug(s"Retrieving statistics for testId: $testId")
    statisticsStore.get(testId)

  override def save(statistics: Statistics): Statistics =
    require(statistics != null, "Statistics cannot be null")
    val testId = generateUniqueId()
    val statisticsWithId = statistics.copy(testId = testId)
    statisticsStore.put(testId, statisticsWithId)
    logger.info(s"Saved statistics with testId: $testId")
    statisticsWithId

  override def deleteAll(userId: String): Boolean =
    require(userId != null && userId.nonEmpty, "UserId cannot be null or empty")
    val initialSize = statisticsStore.size
    statisticsStore.retain((_, stats) => stats.userId != userId)
    val deleted = initialSize != statisticsStore.size
    logger.info(
      s"Deleted all statistics for userId: $userId, success: $deleted"
    )
    deleted

  override def list(userId: String): List[Statistics] =
    require(userId != null && userId.nonEmpty, "UserId cannot be null or empty")
    logger.debug(s"Listing statistics for userId: $userId")
    statisticsStore.values.filter(_.userId == userId).toList

  override def clean(): Boolean =
    logger.warn("Cleaning all statistics from repository")
    statisticsStore.clear()
    true

  @scala.annotation.tailrec
  private def generateUniqueId(): String =
    val id = java.util.UUID.randomUUID().toString
    if statisticsStore.contains(id) then generateUniqueId()
    else id

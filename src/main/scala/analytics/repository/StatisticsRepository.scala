package analytics.repository

import analytics.model.Statistics

trait StatisticsRepository:

  def get(testId: String): Option[Statistics]

  def save(statistics: Statistics): Statistics

  def deleteAll(userId: String): Boolean

  def list(userId: String): List[Statistics]

  def clean(): Boolean

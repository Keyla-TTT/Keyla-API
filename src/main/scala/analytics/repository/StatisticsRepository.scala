package analytics.repository

import analytics.model.Statistics

trait StatisticsRepository:

  def get(id: String): Option[Statistics]

  def create(statistics: Statistics): Statistics

  def deleteAll(id: String): Boolean

  def list(): List[Statistics]

package analytics.calculator

import analytics.model
import analytics.model.{UserAnalytics, UserStatistics}
trait AnalyticsCalculator:

  /** Analyzes user statistics and returns user analytics */
  def analyzeUser(statistics: List[UserStatistics]): UserAnalytics

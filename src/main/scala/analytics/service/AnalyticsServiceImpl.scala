package analytics.service

import analytics.calculator.AnalyticsCalculator
import analytics.model.{Analytics, UserStatistics}

class AnalyticsServiceImpl(private val analyticsCalculator: AnalyticsCalculator)
    extends AnalyticsService:

  override def getUserAnalytics(
      statisticsList: List[UserStatistics]
  ): Analytics =
    analyticsCalculator.analyzeUser(statisticsList)

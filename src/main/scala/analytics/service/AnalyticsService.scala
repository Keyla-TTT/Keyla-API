package analytics.service

import analytics.model.{Analytics, UserStatistics}

trait AnalyticsService:

  def getUserAnalytics(statisticsList: List[UserStatistics]): Analytics

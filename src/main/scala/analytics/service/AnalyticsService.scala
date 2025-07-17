package analytics.service

import analytics.model.{Analytics, UserStatistics}

class AnalyticsService:

  def getUserAnalytics(statisticsList: List[UserStatistics]): Analytics

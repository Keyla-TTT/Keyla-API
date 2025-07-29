package analytics.factory

import analytics.model.{UserAnalytics, UserAnalyticsBuilder}

class UserAnalyticsFactory:
  def createUserAnalytics(
      userId: String,
      totalTests: Int,
      averageWpm: Double,
      averageAccuracy: Double,
      bestWpm: Double,
      worstWpm: Double,
      bestAccuracy: Double,
      worstAccuracy: Double,
      wpmImprovement: Double,
      accuracyImprovement: Double,
      totalErrors: Int,
      averageErrorsPerTest: Double
  ): UserAnalytics =
    new UserAnalyticsBuilder()
      .setUserId(userId)
      .setTotalTests(totalTests)
      .setAverageWpm(averageWpm)
      .setAverageAccuracy(averageAccuracy)
      .setBestWpm(bestWpm)
      .setWorstWpm(worstWpm)
      .setBestAccuracy(bestAccuracy)
      .setWorstAccuracy(worstAccuracy)
      .setWpmImprovement(wpmImprovement)
      .setAccuracyImprovement(accuracyImprovement)
      .setTotalErrors(totalErrors)
      .setAverageErrorsPerTest(averageErrorsPerTest)
      .build()

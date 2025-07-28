package analytics.calculator

import analytics.model.{UserAnalytics, TestStatistics, UserAnalyticsBuilder}

class AnalyticsCalculatorImpl extends AnalyticsCalculator:

  override def analyzeUser(
      statistics: List[TestStatistics],
      userId: String
  ): UserAnalytics =

    if statistics.isEmpty then
      return new UserAnalyticsBuilder()
        .setUserId(userId)
        .setTotalTests(0)
        .setAverageWpm(0.0)
        .setAverageAccuracy(0.0)
        .setBestWpm(0.0)
        .setWorstWpm(0.0)
        .setBestAccuracy(0.0)
        .setWorstAccuracy(0.0)
        .setWpmImprovement(0.0)
        .setAccuracyImprovement(0.0)
        .setTotalErrors(0)
        .setAverageErrorsPerTest(0.0)
        .build()

    val totalTests = statistics.size
    val totalErrors = statistics.flatMap(_.errors).size
    val avgErrorsPerTest = totalErrors.toDouble / totalTests

    val wpms = statistics.map(_.wpm)
    val accuracies = statistics.map(_.accuracy)

    val avgWpm = wpms.sum / totalTests
    val avgAccuracy = accuracies.sum / totalTests
    val bestWpm = wpms.max
    val worstWpm = wpms.min
    val bestAccuracy = accuracies.max
    val worstAccuracy = accuracies.min

    val firstTest = statistics.minBy(_.timestamp)
    val lastTest = statistics.maxBy(_.timestamp)
    val wpmImprovement = lastTest.wpm - firstTest.wpm
    val accuracyImprovement = lastTest.accuracy - firstTest.accuracy

    new UserAnalyticsBuilder()
      .setUserId(userId)
      .setTotalTests(totalTests)
      .setAverageWpm(avgWpm)
      .setAverageAccuracy(avgAccuracy)
      .setBestWpm(bestWpm)
      .setWorstWpm(worstWpm)
      .setBestAccuracy(bestAccuracy)
      .setWorstAccuracy(worstAccuracy)
      .setWpmImprovement(wpmImprovement)
      .setAccuracyImprovement(accuracyImprovement)
      .setTotalErrors(totalErrors)
      .setAverageErrorsPerTest(avgErrorsPerTest)
      .build()

package analytics.calculator

import analytics.model.{UserAnalytics, UserStatistics}

class AnalyticsCalculatorImpl extends AnalyticsCalculator:

  override def analyzeUser(statistics: List[UserStatistics]): UserAnalytics =

    if statistics.isEmpty then
      return UserAnalytics(
        "",
        0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0.0,
        0,
        0.0
      )

    val userId = statistics.head.userId
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

    // Calcolo miglioramenti confrontando primo e ultimo test
    val firstTest = statistics.minBy(_.timestamp)
    val lastTest = statistics.maxBy(_.timestamp)
    val wpmImprovement = lastTest.wpm - firstTest.wpm
    val accuracyImprovement = lastTest.accuracy - firstTest.accuracy

    UserAnalytics(
      userId = userId,
      totalTests = totalTests,
      averageWpm = avgWpm,
      averageAccuracy = avgAccuracy,
      bestWpm = bestWpm,
      worstWpm = worstWpm,
      bestAccuracy = bestAccuracy,
      worstAccuracy = worstAccuracy,
      wpmImprovement = wpmImprovement,
      accuracyImprovement = accuracyImprovement,
      totalErrors = totalErrors,
      averageErrorsPerTest = avgErrorsPerTest
    )

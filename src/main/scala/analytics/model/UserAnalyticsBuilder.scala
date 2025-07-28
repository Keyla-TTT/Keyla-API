package analytics.model

class UserAnalyticsBuilder:

  private var userId: String = ""
  private var totalTests: Int = 0
  private var averageWpm: Double = 0.0
  private var averageAccuracy: Double = 0.0
  private var bestWpm: Double = 0.0
  private var worstWpm: Double = 0.0
  private var bestAccuracy: Double = 0.0
  private var worstAccuracy: Double = 0.0
  private var wpmImprovement: Double = 0.0
  private var accuracyImprovement: Double = 0.0
  private var totalErrors: Int = 0
  private var averageErrorsPerTest: Double = 0.0

  def setUserId(userId: String): UserAnalyticsBuilder =
    this.userId = userId
    this

  def setTotalTests(totalTests: Int): UserAnalyticsBuilder =
    this.totalTests = totalTests
    this

  def setAverageWpm(averageWpm: Double): UserAnalyticsBuilder =
    this.averageWpm = averageWpm
    this

  def setAverageAccuracy(averageAccuracy: Double): UserAnalyticsBuilder =
    this.averageAccuracy = averageAccuracy
    this

  def setBestWpm(bestWpm: Double): UserAnalyticsBuilder =
    this.bestWpm = bestWpm
    this

  def setWorstWpm(worstWpm: Double): UserAnalyticsBuilder =
    this.worstWpm = worstWpm
    this

  def setBestAccuracy(bestAccuracy: Double): UserAnalyticsBuilder =
    this.bestAccuracy = bestAccuracy
    this

  def setWorstAccuracy(worstAccuracy: Double): UserAnalyticsBuilder =
    this.worstAccuracy = worstAccuracy
    this

  def setWpmImprovement(wpmImprovement: Double): UserAnalyticsBuilder =
    this.wpmImprovement = wpmImprovement
    this

  def setAccuracyImprovement(
      accuracyImprovement: Double
  ): UserAnalyticsBuilder =
    this.accuracyImprovement = accuracyImprovement
    this

  def setTotalErrors(totalErrors: Int): UserAnalyticsBuilder =
    this.totalErrors = totalErrors
    this

  def setAverageErrorsPerTest(
      averageErrorsPerTest: Double
  ): UserAnalyticsBuilder =
    this.averageErrorsPerTest = averageErrorsPerTest
    this

  def build(): UserAnalytics =
    UserAnalytics(
      userId = userId,
      totalTests = totalTests,
      averageWpm = averageWpm,
      averageAccuracy = averageAccuracy,
      bestWpm = bestWpm,
      worstWpm = worstWpm,
      bestAccuracy = bestAccuracy,
      worstAccuracy = worstAccuracy,
      wpmImprovement = wpmImprovement,
      accuracyImprovement = accuracyImprovement,
      totalErrors = totalErrors,
      averageErrorsPerTest = averageErrorsPerTest
    )

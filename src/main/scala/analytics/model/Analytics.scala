package analytics.model

trait Analytics:
  def userId: String
  def totalTests: Int
  def averageWpm: Double
  def averageAccuracy: Double
  def bestWpm: Double
  def worstWpm: Double
  def bestAccuracy: Double
  def worstAccuracy: Double
  def wpmImprovement: Double
  def accuracyImprovement: Double
  def totalErrors: Int
  def averageErrorsPerTest: Double

case class UserAnalytics(
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
) extends Analytics

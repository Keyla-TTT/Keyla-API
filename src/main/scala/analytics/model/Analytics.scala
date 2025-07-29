package analytics.model

/** Trait representing comprehensive user analytics and performance insights.
  *
  * This trait defines the structure for storing and accessing aggregated
  * analytics data derived from multiple typing tests. It provides insights into
  * user performance trends, improvement patterns, and overall typing
  * proficiency.
  *
  * =Analytics Metrics=
  *
  *   - '''Performance Averages''': Mean WPM and accuracy across all tests
  *   - '''Performance Extremes''': Best and worst WPM/accuracy values
  *   - '''Improvement Tracking''': Progress indicators over time
  *   - '''Error Analysis''': Total errors and average errors per test
  *
  * =Usage=
  *
  * This trait is typically implemented by concrete classes that store
  * aggregated analytics data, such as `UserAnalytics` for comprehensive user
  * performance reports.
  *
  * @example
  *   {{{
  * val analytics: Analytics = UserAnalytics(
  *   userId = "user-123",
  *   totalTests = 10,
  *   averageWpm = 75.5,
  *   averageAccuracy = 95.2,
  *   bestWpm = 85.0,
  *   worstWpm = 65.0,
  *   bestAccuracy = 98.0,
  *   worstAccuracy = 90.0,
  *   wpmImprovement = 15.5,
  *   accuracyImprovement = 3.2,
  *   totalErrors = 25,
  *   averageErrorsPerTest = 2.5
  * )
  * println(s"User improved WPM by ${analytics.wpmImprovement}%")
  *   }}}
  */
trait Analytics:
  /** Unique identifier of the user
    * @return
    *   String representing the user's unique identifier
    */
  def userId: String

  /** Total number of tests completed by the user
    * @return
    *   Integer representing the total test count
    */
  def totalTests: Int

  /** Average words per minute across all tests
    * @return
    *   Double representing the mean WPM value
    */
  def averageWpm: Double

  /** Average accuracy percentage across all tests
    * @return
    *   Double representing the mean accuracy percentage
    */
  def averageAccuracy: Double

  /** Highest WPM achieved across all tests
    * @return
    *   Double representing the best WPM performance
    */
  def bestWpm: Double

  /** Lowest WPM achieved across all tests
    * @return
    *   Double representing the worst WPM performance
    */
  def worstWpm: Double

  /** Highest accuracy achieved across all tests
    * @return
    *   Double representing the best accuracy performance
    */
  def bestAccuracy: Double

  /** Lowest accuracy achieved across all tests
    * @return
    *   Double representing the worst accuracy performance
    */
  def worstAccuracy: Double

  /** Percentage improvement in WPM from first to last test
    * @return
    *   Double representing WPM improvement percentage
    */
  def wpmImprovement: Double

  /** Percentage improvement in accuracy from first to last test
    * @return
    *   Double representing accuracy improvement percentage
    */
  def accuracyImprovement: Double

  /** Total number of errors made across all tests
    * @return
    *   Integer representing the total error count
    */
  def totalErrors: Int

  /** Average number of errors per test
    * @return
    *   Double representing the mean errors per test
    */
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

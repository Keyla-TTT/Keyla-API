package analytics.calculator

import analytics.model.{Analytics, TestStatistics}

/** Trait for calculating user analytics from typing test statistics.
  *
  * This trait defines the contract for analyzing user performance data and
  * generating comprehensive analytics reports. It processes raw statistics data
  * to produce insights about user typing performance over time.
  *
  * =Analytics Features=
  *
  *   - '''Performance Metrics''': Average WPM, accuracy, and error rates
  *   - '''Trend Analysis''': Improvement tracking over time
  *   - '''Best/Worst Performance''': Peak and low performance identification
  *   - '''Statistical Aggregation''': Summary statistics across multiple tests
  *
  * =Usage=
  *
  * Implementations of this trait should provide meaningful analysis of user
  * statistics to help track progress and identify areas for improvement.
  *
  * @example
  *   {{{
  * val calculator: AnalyticsCalculator = AnalyticsCalculatorImpl()
  * val userStats = List(
  *   TestStatistics("test1", "user123", 75.0, 95.0, List(1, 5), 1000L),
  *   TestStatistics("test2", "user123", 80.0, 97.0, List(2), 2000L)
  * )
  * val analytics = calculator.analyzeUser(userStats)
  * println(s"Average WPM: ${analytics.averageWpm}")
  * println(s"Improvement: ${analytics.wpmImprovement}%")
  *   }}}
  */
trait AnalyticsCalculator:

  /** Analyzes a list of user statistics to generate comprehensive analytics.
    *
    * This method processes multiple test results to calculate various
    * performance metrics including averages, trends, and improvement
    * indicators.
    *
    * @param statistics
    *   List of TestStatistics representing the user's test history
    * @return
    *   Analytics object containing calculated performance metrics
    *
    * @example
    *   {{{
    * val stats = List(
    *   TestStatistics("test1", "user123", 70.0, 90.0, List(1, 3), 1000L),
    *   TestStatistics("test2", "user123", 75.0, 92.0, List(2), 2000L),
    *   TestStatistics("test3", "user123", 80.0, 95.0, List(), 3000L)
    * )
    * val analytics = calculator.analyzeUser(stats)
    * // analytics.averageWpm should be 75.0
    * // analytics.wpmImprovement should be positive
    *   }}}
    */
  def analyzeUser(statistics: List[TestStatistics]): Analytics

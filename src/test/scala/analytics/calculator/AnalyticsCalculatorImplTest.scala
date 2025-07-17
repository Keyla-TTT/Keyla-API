package analytics.calculator

import analytics.model.{UserAnalytics, UserStatistics}
import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers

class AnalyticsCalculatorImplTest extends AnyFunSuite with Matchers:
  private val calculator = AnalyticsCalculatorImpl()

  test("analyzeUser should return default values for empty statistics list") {
    val result = calculator.analyzeUser(List.empty)
    result shouldBe UserAnalytics(
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
  }

  test("analyzeUser should correctly calculate analytics for single test") {
    val stats = UserStatistics(
      userId = "user1",
      testId = "test1",
      wpm = 50.0,
      accuracy = 95.0,
      errors = List("error1", "error2"),
      timestamp = 1000L
    )

    val result = calculator.analyzeUser(List(stats))

    result.userId shouldBe "user1"
    result.totalTests shouldBe 1
    result.averageWpm shouldBe 50.0
    result.averageAccuracy shouldBe 95.0
    result.bestWpm shouldBe 50.0
    result.worstWpm shouldBe 50.0
    result.bestAccuracy shouldBe 95.0
    result.worstAccuracy shouldBe 95.0
    result.wpmImprovement shouldBe 0.0
    result.accuracyImprovement shouldBe 0.0
    result.totalErrors shouldBe 2
    result.averageErrorsPerTest shouldBe 2.0
  }

  test("analyzeUser should correctly calculate analytics for multiple tests") {
    val stats1 =
      UserStatistics("user1", "test1", 40.0, 90.0, List("error1"), 1000L)
    val stats2 = UserStatistics(
      "user1",
      "test2",
      60.0,
      95.0,
      List("error1", "error2"),
      2000L
    )

    val result = calculator.analyzeUser(List(stats1, stats2))

    result.userId shouldBe "user1"
    result.totalTests shouldBe 2
    result.averageWpm shouldBe 50.0
    result.averageAccuracy shouldBe 92.5
    result.bestWpm shouldBe 60.0
    result.worstWpm shouldBe 40.0
    result.bestAccuracy shouldBe 95.0
    result.worstAccuracy shouldBe 90.0
    result.wpmImprovement shouldBe 20.0
    result.accuracyImprovement shouldBe 5.0
    result.totalErrors shouldBe 3
    result.averageErrorsPerTest shouldBe 1.5
  }

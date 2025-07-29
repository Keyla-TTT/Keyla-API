package api.models.analytics

import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

case class AnalyticsResponse(
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
)

object AnalyticsModels:
  given JsonValueCodec[AnalyticsResponse] = JsonCodecMaker.make

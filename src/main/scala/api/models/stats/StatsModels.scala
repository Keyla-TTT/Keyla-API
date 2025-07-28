package api.models.stats

import analytics.model.Statistics
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

case class SaveStatisticsRequest(
    testId: String,
    profileId: String,
    wpm: Double,
    accuracy: Double,
    errors: List[Int],
    timestamp: Long = System.currentTimeMillis()
)

case class StatisticsResponse(
    testId: String,
    profileId: String,
    wpm: Double,
    accuracy: Double,
    errors: List[Int],
    timestamp: Long
)

case class ProfileStatisticsListResponse(
    profileId: String,
    statistics: List[StatisticsResponse]
)

object StatsModels:
  given JsonValueCodec[SaveStatisticsRequest] = JsonCodecMaker.make
  given JsonValueCodec[StatisticsResponse] = JsonCodecMaker.make
  given JsonValueCodec[ProfileStatisticsListResponse] = JsonCodecMaker.make

  def profileStatisticsListToResponse(
      profileId: String,
      statistics: List[Statistics]
  ): ProfileStatisticsListResponse =
    ProfileStatisticsListResponse(
      profileId = profileId,
      statistics = statistics.map(stat => statisticsToResponse(stat))
    )

  def statisticsToResponse(statistics: Statistics): StatisticsResponse =
    StatisticsResponse(
      testId = statistics.testId,
      profileId = statistics.userId,
      wpm = statistics.wpm,
      accuracy = statistics.accuracy,
      errors = statistics.errors,
      timestamp = statistics.timestamp
    )

package api.endpoints.stats

import api.models.common.{CommonModels, ErrorResponse}
import api.models.common.CommonModels.given
import api.models.stats.StatsModels
import api.models.stats.StatsModels.given
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.jsoniter.*

object StatsEndpoints:

  private val errorMapping = oneOf[ErrorResponse](
    oneOfVariantValueMatcher(
      statusCode(StatusCode.BadRequest).and(jsonBody[ErrorResponse])
    ) {
      case (err: ErrorResponse) if err.statusCode == 400 => true
      case _                                             => false
    },
    oneOfVariantValueMatcher(
      statusCode(StatusCode.NotFound).and(jsonBody[ErrorResponse])
    ) {
      case (err: ErrorResponse) if err.statusCode == 404 => true
      case _                                             => false
    },
    oneOfVariantValueMatcher(
      statusCode(StatusCode.UnprocessableEntity).and(jsonBody[ErrorResponse])
    ) {
      case (err: ErrorResponse) if err.statusCode == 422 => true
      case _                                             => false
    },
    oneOfVariantValueMatcher(
      statusCode(StatusCode.InternalServerError).and(jsonBody[ErrorResponse])
    ) {
      case (err: ErrorResponse) if err.statusCode == 500 => true
      case _                                             => false
    },
    oneOfVariantValueMatcher(
      statusCode(StatusCode.ServiceUnavailable).and(jsonBody[ErrorResponse])
    ) {
      case (err: ErrorResponse) if err.statusCode == 503 => true
      case _                                             => false
    }
  )

  private val baseEndpoint = endpoint
    .tag("Statistics API")
    .errorOut(errorMapping)

  val saveStatistics: Endpoint[
    Unit,
    api.models.stats.SaveStatisticsRequest,
    ErrorResponse,
    api.models.stats.StatisticsResponse,
    Any
  ] =
    baseEndpoint.post
      .in(
        "api" / "stats"
      )
      .in(
        jsonBody[api.models.stats.SaveStatisticsRequest].description(
          "Statistics data to save"
        )
      )
      .out(
        jsonBody[api.models.stats.StatisticsResponse].description(
          "Successfully saved test statistics"
        )
      )
      .summary("Save test statistics")
      .description(
        "Saves the provided test's statistics for a specific user profile"
      )

  val getAllProfileStatistics: Endpoint[
    Unit,
    String,
    ErrorResponse,
    api.models.stats.ProfileStatisticsListResponse,
    Any
  ] =
    baseEndpoint.get
      .in(
        "api" / "stats" / path[String]("userId").description(
          "Test identifier"
        )
      )
      .out(
        jsonBody[api.models.stats.ProfileStatisticsListResponse].description(
          "List of all profile statistics"
        )
      )
      .summary("Get all profile statistics")
      .description(
        "Retrieves all available statistics for a specific user profile"
      )

package api.endpoints.analytics

import api.models.analytics.AnalyticsModels
import api.models.analytics.AnalyticsModels.given
import api.models.common.{CommonModels, ErrorResponse}
import api.models.common.CommonModels.given
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.jsoniter.*

object AnalyticsEndpoints:

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
    .tag("Analytics API")
    .errorOut(errorMapping)

  val getUserAnalytics: PublicEndpoint[
    String,
    ErrorResponse,
    api.models.analytics.AnalyticsResponse,
    Any
  ] =
    baseEndpoint.get
      .in(
        "api" / "analytics" / path[String]("userId").description(
          "User identifier"
        )
      )
      .out(
        jsonBody[api.models.analytics.AnalyticsResponse].description(
          "User analytics data"
        )
      )
      .summary("Get user analytics")
      .description(
        "Retrieves comprehensive analytics data for a specific user"
      )

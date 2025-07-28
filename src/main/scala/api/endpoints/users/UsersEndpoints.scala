package api.endpoints.users

import api.models.common.{CommonModels, ErrorResponse}
import api.models.common.CommonModels.given
import api.models.users.UsersModels
import api.models.users.UsersModels.given
import sttp.model.StatusCode
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.jsoniter.*

object UsersEndpoints:

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
    .tag("Users API")
    .errorOut(errorMapping)

  val createProfile: PublicEndpoint[
    api.models.users.CreateProfileRequest,
    ErrorResponse,
    api.models.users.ProfileResponse,
    Any
  ] =
    baseEndpoint.post
      .in("api" / "profiles")
      .in(
        jsonBody[api.models.users.CreateProfileRequest]
          .description("Profile creation request")
      )
      .out(statusCode(StatusCode.Created))
      .out(
        jsonBody[api.models.users.ProfileResponse]
          .description("Successfully created profile")
      )
      .summary("Create a new user profile")
      .description("Creates a new user profile with the provided information")

  val getAllProfiles: PublicEndpoint[
    Unit,
    ErrorResponse,
    api.models.users.ProfileListResponse,
    Any
  ] =
    baseEndpoint.get
      .in("api" / "profiles")
      .out(
        jsonBody[api.models.users.ProfileListResponse]
          .description("List of all profiles")
      )
      .summary("Get all user profiles")
      .description("Retrieves all user profiles in the system")

  val getProfileById: PublicEndpoint[
    String,
    ErrorResponse,
    api.models.users.ProfileResponse,
    Any
  ] =
    baseEndpoint.get
      .in(
        "api" / "profiles" / path[String]("profileId").description(
          "Unique profile identifier"
        )
      )
      .out(
        jsonBody[api.models.users.ProfileResponse]
          .description("Profile details")
      )
      .summary("Get a user profile by ID")
      .description("Retrieves a specific user profile by its unique identifier")

  val updateProfile: PublicEndpoint[
    (String, api.models.users.CreateProfileRequest),
    ErrorResponse,
    api.models.users.ProfileResponse,
    Any
  ] =
    baseEndpoint.put
      .in(
        "api" / "profiles" / path[String]("profileId").description(
          "Unique profile identifier"
        )
      )
      .in(
        jsonBody[api.models.users.CreateProfileRequest]
          .description("Profile update request")
      )
      .out(
        jsonBody[api.models.users.ProfileResponse]
          .description("Updated profile details")
      )
      .summary("Update a user profile")
      .description("Updates an existing user profile with new information")

  val deleteProfile: PublicEndpoint[
    String,
    ErrorResponse,
    api.models.users.DeleteProfileResponse,
    Any
  ] =
    baseEndpoint.delete
      .in(
        "api" / "profiles" / path[String]("profileId").description(
          "Unique profile identifier"
        )
      )
      .out(
        jsonBody[api.models.users.DeleteProfileResponse]
          .description("Deletion result")
      )
      .summary("Delete a user profile")
      .description("Deletes a user profile by its unique identifier")

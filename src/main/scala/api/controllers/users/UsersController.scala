package api.controllers.users

import api.models.AppError
import api.models.AppResult
import api.models.users.*
import api.services.ProfileService
import cats.effect.IO
import config.*

/** Controller for handling user profile-related HTTP requests.
  *
  * This controller provides the HTTP interface for user profile operations,
  * delegating business logic to the ProfileService. It handles request/response
  * transformation and error handling for the REST API endpoints.
  *
  * The controller follows the standard pattern of delegating to service layers
  * and transforming results to appropriate HTTP responses.
  */
class UsersController(
    profileService: ProfileService
):

  private def handleServiceResult[A](
      serviceCall: AppResult[A]
  ): IO[Either[AppError, A]] =
    serviceCall.value

  /** Creates a new user profile.
    *
    * @param request
    *   The profile creation request
    * @return
    *   IO containing either the created profile response or an error
    */
  def createProfile(
      request: CreateProfileRequest
  ): IO[Either[AppError, ProfileResponse]] =
    handleServiceResult(profileService.createProfile(request))

  /** Retrieves all user profiles.
    *
    * @return
    *   IO containing either the list of all profiles or an error
    */
  def getAllProfiles(): IO[Either[AppError, ProfileListResponse]] =
    handleServiceResult(profileService.getAllProfiles())

  /** Retrieves a specific user profile by ID.
    *
    * @param profileId
    *   The unique identifier of the profile
    * @return
    *   IO containing either the profile response or an error
    */
  def getProfileById(profileId: String): IO[Either[AppError, ProfileResponse]] =
    handleServiceResult(profileService.getProfileById(profileId))

  /** Updates an existing user profile.
    *
    * @param profileId
    *   The unique identifier of the profile to update
    * @param request
    *   The profile update request
    * @return
    *   IO containing either the updated profile response or an error
    */
  def updateProfile(
      profileId: String,
      request: CreateProfileRequest
  ): IO[Either[AppError, ProfileResponse]] =
    handleServiceResult(profileService.updateProfile(profileId, request))

  /** Deletes a user profile.
    *
    * @param profileId
    *   The unique identifier of the profile to delete
    * @return
    *   IO containing either the delete response or an error
    */
  def deleteProfile(
      profileId: String
  ): IO[Either[AppError, DeleteProfileResponse]] =
    handleServiceResult(profileService.deleteProfile(profileId))

package api.services

import api.models.AppError.*
import api.models.AppResult
import api.models.users.*
import cats.effect.IO
import users_management.model.{Profile, UserProfile}
import users_management.repository.ProfileRepository

/** API-level service for managing user profiles with comprehensive error
  * handling.
  *
  * This service provides the main interface for profile operations in the Keyla
  * typing test application. It handles user profile creation, retrieval, and
  * management with proper error handling and business logic validation.
  *
  * The service orchestrates interactions with the profile repository and
  * handles data transformation between domain models and API response models.
  * All operations return `AppResult[T]` which provides consistent error
  * handling and composability.
  *
  * =Key Features=
  *
  *   - '''Profile Creation''': Create new user profiles with validation
  *   - '''Profile Retrieval''': Get individual profiles or list all profiles
  *   - '''Profile Updates''': Update existing profile information
  *   - '''Profile Deletion''': Remove profiles with proper cleanup
  *   - '''Error Handling''': Comprehensive error handling with typed errors
  *   - '''Data Transformation''': Convert between domain and API models
  *
  * =Service Architecture=
  *
  * The service follows a layered architecture:
  * {{{
  * REST API Controller
  *        ↓
  * ProfileService (this layer)
  *        ↓
  * ProfileRepository
  *        ↓
  * Data Storage (MongoDB or In-Memory)
  * }}}
  *
  * =Error Handling=
  *
  * All service methods return `AppResult[T]` which encapsulates either success
  * or typed errors:
  *   - '''Profile Errors''': ProfileNotFound, ProfileCreationFailed
  *   - '''Database Errors''': DatabaseError
  *   - '''Validation Errors''': ValidationError
  *
  * @example
  *   {{{
  * // Create a profile service
  * val service = ProfileService(profileRepository)
  *
  * // Create a user profile
  * val request = CreateProfileRequest("John Doe", "john@example.com", Set("user"))
  * val profileResult = service.createProfile(request)
  *
  * // Get all profiles
  * val profilesResult = service.getAllProfiles()
  *
  * // Get a specific profile
  * val profileResult = service.getProfileById("profile-123")
  *   }}}
  */
trait ProfileService:

  /** Creates a new user profile with the provided information.
    *
    * This operation validates the input data and creates a new profile in the
    * system. The profile will be assigned a unique identifier and returned with
    * all system-generated fields populated.
    *
    * @param request
    *   The profile creation request containing name, email, and settings
    * @return
    *   AppResult containing the created profile response or an error
    *
    * @example
    *   {{{
    * val request = CreateProfileRequest(
    *   name = "Alice Johnson",
    *   email = "alice@example.com",
    *   settings = Set("user", "premium")
    * )
    * service.createProfile(request).value.map {
    *   case Right(response) => println(s"Created profile: ${response.id}")
    *   case Left(error) => println(s"Failed: ${error.message}")
    * }
    *   }}}
    */
  def createProfile(request: CreateProfileRequest): AppResult[ProfileResponse]

  /** Retrieves all user profiles in the system.
    *
    * This operation fetches all profiles from the repository and transforms
    * them into API response format. Returns an empty list if no profiles exist.
    *
    * @return
    *   AppResult containing the list of all profiles or an error
    *
    * @example
    *   {{{
    * service.getAllProfiles().value.map {
    *   case Right(response) => println(s"Found ${response.profiles.length} profiles")
    *   case Left(error) => println(s"Error: ${error.message}")
    * }
    *   }}}
    */
  def getAllProfiles(): AppResult[ProfileListResponse]

  /** Retrieves a specific user profile by its unique identifier.
    *
    * This operation fetches a single profile from the repository. If the
    * profile is not found, returns a ProfileNotFound error.
    *
    * @param profileId
    *   The unique identifier of the profile to retrieve
    * @return
    *   AppResult containing the profile response or an error
    *
    * @example
    *   {{{
    * service.getProfileById("profile-123").value.map {
    *   case Right(response) => println(s"Found profile: ${response.name}")
    *   case Left(ProfileNotFound(id)) => println(s"Profile $id not found")
    *   case Left(error) => println(s"Error: ${error.message}")
    * }
    *   }}}
    */
  def getProfileById(profileId: String): AppResult[ProfileResponse]

  /** Updates an existing user profile with new information.
    *
    * This operation validates the input data and updates the profile in the
    * system. If the profile is not found, returns a ProfileNotFound error.
    *
    * @param profileId
    *   The unique identifier of the profile to update
    * @param request
    *   The profile update request containing the new information
    * @return
    *   AppResult containing the updated profile response or an error
    *
    * @example
    *   {{{
    * val request = CreateProfileRequest(
    *   name = "Alice Smith",
    *   email = "alice.smith@example.com",
    *   settings = Set("user", "admin")
    * )
    * service.updateProfile("profile-123", request).value.map {
    *   case Right(response) => println(s"Updated profile: ${response.name}")
    *   case Left(ProfileNotFound(id)) => println(s"Profile $id not found")
    *   case Left(error) => println(s"Error: ${error.message}")
    * }
    *   }}}
    */
  def updateProfile(
      profileId: String,
      request: CreateProfileRequest
  ): AppResult[ProfileResponse]

  /** Deletes a user profile by its unique identifier.
    *
    * This operation removes the profile from the system. If the profile is not
    * found, returns a ProfileNotFound error.
    *
    * @param profileId
    *   The unique identifier of the profile to delete
    * @return
    *   AppResult containing the delete response or an error
    *
    * @example
    *   {{{
    * service.deleteProfile("profile-123").value.map {
    *   case Right(response) => println(s"Profile deleted: ${response.success}")
    *   case Left(ProfileNotFound(id)) => println(s"Profile $id not found")
    *   case Left(error) => println(s"Error: ${error.message}")
    * }
    *   }}}
    */
  def deleteProfile(profileId: String): AppResult[DeleteProfileResponse]

/** Companion object for ProfileService providing factory methods.
  */
object ProfileService:

  /** Creates a new ProfileService instance with the provided repository.
    *
    * @param profileRepository
    *   Repository for user profile operations
    * @return
    *   A configured ProfileService instance
    *
    * @example
    *   {{{
    * val service = ProfileService(profileRepository)
    *   }}}
    */
  def apply(profileRepository: ProfileRepository): ProfileService =
    ProfileServiceImpl(profileRepository)

/** Default implementation of ProfileService using the provided repository
  * implementation for data persistence.
  *
  * This implementation:
  *   - Handles data transformation between domain and API models
  *   - Provides comprehensive error handling and validation
  *   - Ensures data consistency across repository operations
  *   - Uses AppResult for consistent error handling
  *
  * @param profileRepository
  *   Repository for user profile data persistence
  */
class ProfileServiceImpl(
    profileRepository: ProfileRepository
) extends ProfileService:

  def createProfile(request: CreateProfileRequest): AppResult[ProfileResponse] =
    for
      newProfile <- AppResult.pure(
        UserProfile(
          id = None,
          name = request.name,
          email = request.email,
          settings = request.settings
        )
      )
      savedProfile <- AppResult.attemptBlocking(
        IO.blocking(profileRepository.create(newProfile))
      )(error => ProfileCreationFailed(error.getMessage))
      response <- AppResult.pure(UsersModels.profileToResponse(savedProfile))
    yield response

  def getAllProfiles(): AppResult[ProfileListResponse] =
    AppResult
      .attemptBlocking(
        IO.blocking(profileRepository.list())
      )(error => DatabaseError("profiles lookup", error.getMessage))
      .map(UsersModels.profileListToResponse)

  def getProfileById(profileId: String): AppResult[ProfileResponse] =
    for
      profile <- AppResult.fromOptionF(
        AppResult
          .attemptBlocking(
            IO.blocking(profileRepository.get(profileId))
          )(error => DatabaseError("profile lookup", error.getMessage))
          .value
          .map(_.toOption.flatten),
        ProfileNotFound(profileId)
      )
      response <- AppResult.pure(UsersModels.profileToResponse(profile))
    yield response

  def updateProfile(
      profileId: String,
      request: CreateProfileRequest
  ): AppResult[ProfileResponse] =
    for
      existingProfile <- getProfileById(profileId)
      updatedProfile <- AppResult.pure(
        UserProfile(
          id = Some(profileId),
          name = request.name,
          email = request.email,
          settings = request.settings
        )
      )
      savedProfile <- AppResult.fromOptionF(
        AppResult
          .attemptBlocking(
            IO.blocking(profileRepository.update(updatedProfile))
          )(error => DatabaseError("profile update", error.getMessage))
          .value
          .map(_.toOption.flatten),
        ProfileNotFound(profileId)
      )
      response <- AppResult.pure(UsersModels.profileToResponse(savedProfile))
    yield response

  def deleteProfile(profileId: String): AppResult[DeleteProfileResponse] =
    for
      _ <- getProfileById(profileId)
      deleted <- AppResult.attemptBlocking(
        IO.blocking(profileRepository.delete(profileId))
      )(error => DatabaseError("profile deletion", error.getMessage))
      response <- AppResult.pure(UsersModels.deleteProfileToResponse(deleted))
    yield response

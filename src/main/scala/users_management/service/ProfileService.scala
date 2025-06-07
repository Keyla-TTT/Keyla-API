package users_management.service

import users_management.model.{Profile, UserProfile}

/** Service trait for managing user profiles business logic.
 *
 * This trait defines the high-level operations available for managing user profiles,
 * abstracting the underlying repository implementation details.
 */
trait ProfileService:

  /** Retrieves a profile by its ID.
   *
   * @param id The unique identifier of the profile to retrieve
   * @return Some(Profile) if found, None if not found
   */
  def getProfile(id: String): Option[Profile]

  /** Creates a new user profile.
   *
   * @param userProfile The user profile to create
   * @return The created profile with any system-generated fields populated
   */
  def createProfile(userProfile: UserProfile): Profile

  /** Updates an existing user profile.
   *
   * @param userProfile The profile with updated information
   * @return Some(Profile) if the profile was updated, None if not found
   */
  def updateProfile(userProfile: UserProfile): Option[Profile]

  /** Deletes a profile by its ID.
   *
   * @param id The unique identifier of the profile to delete
   * @return true if the profile was deleted, false if not found
   */
  def deleteProfile(id: String): Boolean

  /** Deletes all profiles from the system.
   *
   * @return true if any profiles were deleted, false if system was empty
   */
  def deleteAllProfiles(): Boolean

  /** Lists all profiles in the system.
   *
   * @return A list containing all profiles
   */
  def listProfiles(): List[Profile]
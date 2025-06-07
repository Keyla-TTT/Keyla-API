package users_management.repository

import users_management.model.Profile

/** Repository trait for managing user profiles.
 *
 * This trait defines the contract for storing and retrieving Profile entities.
 * Implementations of this trait should handle the persistence layer operations
 * for user profiles (e.g. database access).
 */
trait ProfileRepository:

  /** Retrieves a profile by its ID.
   *
   * @param id The unique identifier of the profile to retrieve
   * @return Some(Profile) if found, None if not found
   */
  def get(id: String): Option[Profile]

  /** Creates a new profile in the repository.
   *
   * @param profile The profile to create
   * @return The created profile with any system-generated fields populated
   */
  def create(profile: Profile): Profile

  /** Updates an existing profile.
   *
   * @param profile The profile with updated information
   * @return Some(Profile) if the profile was updated, None if not found
   */
  def update(profile: Profile): Option[Profile]

  /** Deletes a profile by its ID.
   *
   * @param id The unique identifier of the profile to delete
   * @return true if the profile was deleted, false if not found
   */
  def delete(id: String): Boolean

  /** Deletes all profiles from the repository.
   *
   * @return true if any profiles were deleted, false if repository was empty
   */
  def deleteAll(): Boolean

  /** Lists all profiles in the repository.
   *
   * @return A list containing all profiles
   */
  def list(): List[Profile]
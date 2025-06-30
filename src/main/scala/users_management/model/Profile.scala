package users_management.model

/** Trait representing a user profile in the system.
  *
  * This trait defines the basic structure and behavior for all user profiles.
  */
trait Profile:
  /** Unique identifier of the profile */
  def id: Option[String]

  /** Name of the user */
  def name: String

  /** Email address of the user */
  def email: String

  /** Set of user settings/permissions */
  def settings: Set[String]

  /** Creates a copy of this profile with optional field updates
    *
    * @param id
    *   The new id or current if not specified
    * @param name
    *   The new name or current if not specified
    * @param email
    *   The new email or current if not specified
    * @param settings
    *   The new settings or current if not specified
    * @return
    *   A new Profile instance with the updated fields
    */
  def copy(
      id: Option[String] = id,
      name: String = name,
      email: String = email,
      settings: Set[String] = settings
  ): Profile

/** Concrete implementation of a user profile.
  *
  * @param id
  *   Unique identifier of the profile
  * @param name
  *   Name of the user
  * @param email
  *   Email address of the user
  * @param settings
  *   Set of user settings/permissions
  */
case class UserProfile(
    override val id: Option[String],
    override val name: String,
    override val email: String,
    override val settings: Set[String]
) extends Profile:
  /** Creates a copy of this UserProfile with optional field updates
    *
    * @param id
    *   The new id or current if not specified
    * @param name
    *   The new name or current if not specified
    * @param email
    *   The new email or current if not specified
    * @param settings
    *   The new settings or current if not specified
    * @return
    *   A new UserProfile instance with the updated fields
    */
  override def copy(
      id: Option[String] = id,
      name: String = name,
      email: String = email,
      settings: Set[String] = settings
  ): UserProfile = UserProfile(id, name, email, settings)

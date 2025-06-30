package users_management.factory

import users_management.model.*

/** Factory class for creating UserProfile instances.
  *
  * This factory provides methods to create user profiles with consistent
  * initialization using the builder pattern.
  */
class UserFactory:

  /** Creates a new UserProfile instance with the specified attributes.
    *
    * @param name
    *   The name of the user
    * @param email
    *   The email address of the user
    * @param password
    *   The user's password (will be hashed by the builder)
    * @param settings
    *   Set of user settings/permissions (defaults to empty set)
    * @return
    *   A new UserProfile instance with the provided attributes
    */
  def createUserProfile(
      name: String,
      email: String,
      password: String,
      settings: Set[String] = Set()
  ): UserProfile =
    UserProfileBuilder()
      .withName(name)
      .withEmail(email)
      .withPassword(password)
      .withSettings(settings)


      .build()

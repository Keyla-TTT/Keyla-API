package users_management.factory

import users_management.model._

class UserFactory:

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
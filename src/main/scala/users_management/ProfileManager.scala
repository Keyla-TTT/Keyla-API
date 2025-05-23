package users_management

import java.util.UUID

trait ProfileManager:
  def getProfile(id: UUID): Option[Profile]
  def createProfile(name: String, email: String, password: String, settings: Set[String]): Profile
  def updateProfile(id: UUID, name: Option[String], email: Option[String], password: Option[String], settings: Option[Set[String]]): Option[Profile]
  def deleteProfile(id: UUID): Boolean
  def listProfiles(): List[Profile]

case class UserProfileManager() extends ProfileManager:
  private var profiles: Map[UUID, Profile] = Map()

  def getProfile(id: UUID): Option[Profile] =
    profiles.get(id)

  def createProfile(name: String, email: String, password: String, settings: Set[String]): Profile =
    val profile = UserProfile(UUID.randomUUID(), name, email, password, settings)
    profiles += (profile.getId -> profile)
    profile

  def updateProfile(id: UUID, name: Option[String], email: Option[String], password: Option[String], settings: Option[Set[String]]): Option[Profile] =
    profiles.get(id).map { profile =>
      val updatedProfile = profile.copy(
        name = name.getOrElse(profile.getName),
        email = email.getOrElse(profile.getEmail),
        password = password.getOrElse(profile.getPassword),
        settings = settings.getOrElse(profile.getSettings)
      )
      profiles += (updatedProfile.getId -> updatedProfile)
      updatedProfile
    }

  def deleteProfile(id: UUID): Boolean =
    if (profiles.contains(id)) {
      profiles -= id
      true
    } else {
      false
    }

  def listProfiles(): List[Profile] =
    profiles.values.toList
    
  

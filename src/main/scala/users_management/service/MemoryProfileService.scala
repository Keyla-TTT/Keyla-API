package users_management.service

import users_management.model.{Profile, ProfileManager, UserProfile}
import users_management.repository.ProfileRepository


case class MemoryProfileService(private val profiles: ProfileRepository) extends ProfileManager:
  
  def getProfile(id: String): Option[Profile] =
    profiles.get(id)
  
  def createProfile(userProfile: UserProfile): Profile =
    profiles.create(userProfile)
  
  def updateProfile(userProfile: UserProfile): Option[Profile] =
    profiles.update(userProfile)
  
  def deleteProfile(id: String): Boolean =
    profiles.delete(id)
  
  def listProfiles(): List[Profile] =
    profiles.list()
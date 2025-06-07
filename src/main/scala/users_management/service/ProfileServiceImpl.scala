package users_management.service

import users_management.model.{Profile, UserProfile}
import users_management.repository.ProfileRepository

case class ProfileServiceImpl(private val profiles: ProfileRepository) extends ProfileService:
  
  override def getProfile(id: String): Option[Profile] =
    profiles.get(id)
  
  override def createProfile(userProfile: UserProfile): Profile =
    profiles.create(userProfile)
  
  override def updateProfile(userProfile: UserProfile): Option[Profile] =
    profiles.update(userProfile)
  
  override def deleteProfile(id: String): Boolean =
    profiles.delete(id)
    
  override def deleteAllProfiles(): Boolean =
    profiles.deleteAll()
  
  override def listProfiles(): List[Profile] =
    profiles.list()
package users_management.service

import users_management.model.{Profile, UserProfile}

trait ProfileService:
  
  def getProfile(id: String): Option[Profile]

  def createProfile(userProfile: UserProfile): Profile

  def updateProfile(userProfile: UserProfile): Option[Profile]

  def deleteProfile(id: String): Boolean
  
  def deleteAllProfiles(): Boolean

  def listProfiles(): List[Profile]

    
  

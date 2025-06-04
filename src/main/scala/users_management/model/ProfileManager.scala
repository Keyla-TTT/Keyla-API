package users_management.model


trait ProfileManager:
  
  def getProfile(id: String): Option[Profile]

  def createProfile(userProfile: UserProfile): Profile

  def updateProfile(userProfile: UserProfile): Option[Profile]

  def deleteProfile(id: String): Boolean

  def listProfiles(): List[Profile]

    
  

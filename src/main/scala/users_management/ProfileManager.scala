package users_management

import java.util.UUID

trait ProfileManager:
  def getProfile(id: UUID): Option[Profile]
  def createProfile(name: String, email: String, password: String, settings: Set[String]): Profile
  def updateProfile(id: UUID, name: Option[String], email: Option[String], password: Option[String], settings: Option[Set[String]]): Option[Profile]
  def deleteProfile(id: UUID): Boolean
  def listProfiles(): List[Profile]

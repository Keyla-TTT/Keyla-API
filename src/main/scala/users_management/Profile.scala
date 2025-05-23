package users_management
import java.util.UUID

trait Profile:
  def getId: UUID
  def getName: String
  def getEmail: String
  def getPassword: String
  def getSettings: Set[String]



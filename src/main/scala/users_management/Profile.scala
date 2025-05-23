package users_management
import java.util.UUID


trait Profile:
  def getId: UUID
  def getName: String
  def getEmail: String
  def getPassword: String
  def getSettings: Set[String]
  def copy
    (id: UUID = this.getId,
     name: String = this.getName,
     email: String = this.getEmail,
     password: String = this.getPassword,
     settings: Set[String] = this.getSettings): Profile


case class UserProfile(
  id: UUID,
  name: String,
  email: String,
  password: String,
  settings: Set[String]
) extends Profile:
  def getId: UUID = id
  def getName: String = name
  def getEmail: String = email
  def getPassword: String = password
  def getSettings: Set[String] = settings
  def copy(
    id: UUID = this.id,
    name: String = this.name,
    email: String = this.email,
    password: String = this.password,
    settings: Set[String] = this.settings
  ): UserProfile =
    UserProfile(id, name, email, password, settings)


package users_management.model

trait Profile:
  def id: Option[String]
  def name: String
  def email: String
  def password: String
  def settings: Set[String]
  def copy(
    id: Option[String] = id,
    name: String = name,
    email: String = email,
    password: String = password,
    settings: Set[String] = settings
  ): Profile

case class UserProfile(
  override val id: Option[String],
  override val name: String,
  override val email: String,
  override val password: String,
  override val settings: Set[String]
) extends Profile:
    override def copy(
        id: Option[String] = id,
        name: String = name,
        email: String = email,
        password: String = password,
        settings: Set[String] = settings): UserProfile = UserProfile(id, name, email, password, settings)


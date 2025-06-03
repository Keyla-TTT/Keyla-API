package users_management.model

trait Profile:
  def id: Option[String]
  def name: String
  def email: String
  def password: String
  def settings: Set[String]

case class UserProfile(
  override val id: Option[String],
  override val name: String,
  override val email: String,
  override val password: String,
  override val settings: Set[String]
) extends Profile

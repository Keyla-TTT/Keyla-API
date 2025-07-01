package users_management.model

class UserProfileBuilder:
  private var id: Option[String] = None
  private var name: String = ""
  private var email: String = ""
  private var settings: Set[String] = Set()

  def withId(id: String): UserProfileBuilder =
    this.id = Some(id)
    this

  def withName(name: String): UserProfileBuilder =
    this.name = name
    this

  def withEmail(email: String): UserProfileBuilder =
    this.email = email
    this

  def withSettings(settings: Set[String]): UserProfileBuilder =
    this.settings = settings
    this

  def build(): UserProfile =
    UserProfile(id, name, email, settings)

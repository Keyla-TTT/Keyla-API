package users_management.factory

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.Matcher
import org.scalatest.matchers.should.Matchers
import users_management.factory.UserFactory

class TestFactory extends AnyFunSuite, Matchers:

  val validName = "Mario Rossi"
  val validEmail = "mario.rossi@email.com"
  val validSettings: Set[String] = Set("dark_mode", "notifications")

  def profileMatcher(
      name: String,
      email: String,
      settings: Set[String]
  ): Matcher[Object] =
    have(
      Symbol("name")(name),
      Symbol("email")(email),
      Symbol("settings")(settings)
    )

  val factory: UserFactory = UserFactory()

  test("createUserProfile restituisce UserProfile con dati validi") {
    val profile = factory.createUserProfile(
      validName,
      validEmail,
      validSettings
    )
    profile should profileMatcher(
      validName,
      validEmail,
      validSettings
    )
  }

  test(
    "createUserProfile restituisce UserProfile con settings vuoto se non specificato"
  ) {
    val profile =
      factory.createUserProfile(validName, validEmail)
    profile.settings shouldBe empty
  }

  test("createUserProfile gestisce nome vuoto") {
    val profile =
      factory.createUserProfile("", validEmail, validSettings)
    profile.name shouldBe ""
  }

  test("createUserProfile gestisce email vuota") {
    val profile =
      factory.createUserProfile(validName, "", validSettings)
    profile.email shouldBe ""
  }

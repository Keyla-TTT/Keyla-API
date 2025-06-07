package users_management.factory

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import users_management.factory.UserFactory

class TestFactory extends AnyFunSuite, Matchers:

  val validName = "Mario Rossi"
  val validEmail = "mario.rossi@email.com"
  val validPassword = "password123"
  val validSettings = Set("dark_mode", "notifications")

  def profileMatcher(name: String, email: String, password: String, settings: Set[String]) =
    have (
      Symbol("name")(name),
      Symbol("email")(email),
      Symbol("password")(password),
      Symbol("settings")(settings)
    )

  val factory = UserFactory()

  test("createUserProfile restituisce UserProfile con dati validi") {
    val profile = factory.createUserProfile(validName, validEmail, validPassword, validSettings)
    profile should profileMatcher(validName, validEmail, validPassword, validSettings)
  }

  test("createUserProfile restituisce UserProfile con settings vuoto se non specificato") {
    val profile = factory.createUserProfile(validName, validEmail, validPassword)
    profile.settings shouldBe empty
  }

  test("createUserProfile gestisce nome vuoto") {
    val profile = factory.createUserProfile("", validEmail, validPassword, validSettings)
    profile.name shouldBe ""
  }

  test("createUserProfile gestisce email vuota") {
    val profile = factory.createUserProfile(validName, "", validPassword, validSettings)
    profile.email shouldBe ""
  }

  test("createUserProfile gestisce password vuota") {
    val profile = factory.createUserProfile(validName, validEmail, "", validSettings)
    profile.password shouldBe ""
  }

  test("createUserProfile gestisce settings vuoto") {
    val profile = factory.createUserProfile(validName, validEmail, validPassword, Set())
    profile.settings shouldBe empty
  }
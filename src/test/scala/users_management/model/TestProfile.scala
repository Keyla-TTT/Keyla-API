package users_management.model

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.Matcher
import org.scalatest.matchers.should.Matchers
import users_management.model.UserProfile

class TestProfile extends AnyFunSuite, Matchers:

  def validName = "Mario Rossi"
  def validEmail = "mario.rossi@email.com"
  def validSettings: Set[String] = Set("dark_mode", "notifications")

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

  test("UserProfile con dati validi") {
    val profile =
      UserProfile(None, validName, validEmail, validSettings)
    profile should profileMatcher(
      validName,
      validEmail,
      validSettings
    )
  }

  test("UserProfile con settings vuoto") {
    val profile = UserProfile(None, validName, validEmail, Set())
    profile.settings shouldBe empty
  }

  test("UserProfile con nome vuoto") {
    val profile =
      UserProfile(None, "", validEmail, validSettings)
    profile.name shouldBe ""
  }

  test("UserProfile con email vuota") {
    val profile = UserProfile(None, validName, "", validSettings)
    profile.email shouldBe ""
  }

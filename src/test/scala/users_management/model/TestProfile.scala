package users_management.model

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import users_management.model.UserProfile
import org.scalatest.matchers.Matcher

class TestProfile extends AnyFunSuite, Matchers:

  def validName = "Mario Rossi"
  def validEmail = "mario.rossi@email.com"
  def validPassword = "password123"
  def validSettings: Set[String] = Set("dark_mode", "notifications")

  def profileMatcher(
      name: String,
      email: String,
      password: String,
      settings: Set[String]
  ): Matcher[Object] =
    have(
      Symbol("name")(name),
      Symbol("email")(email),
      Symbol("password")(password),
      Symbol("settings")(settings)
    )

  test("UserProfile con dati validi") {
    val profile =
      UserProfile(None, validName, validEmail, validPassword, validSettings)
    profile should profileMatcher(
      validName,
      validEmail,
      validPassword,
      validSettings
    )
  }

  test("UserProfile con settings vuoto") {
    val profile = UserProfile(None, validName, validEmail, validPassword, Set())
    profile.settings shouldBe empty
  }

  test("UserProfile con nome vuoto") {
    val profile =
      UserProfile(None, "", validEmail, validPassword, validSettings)
    profile.name shouldBe ""
  }

  test("UserProfile con email vuota") {
    val profile = UserProfile(None, validName, "", validPassword, validSettings)
    profile.email shouldBe ""
  }

  test("UserProfile con password vuota") {
    val profile = UserProfile(None, validName, validEmail, "", validSettings)
    profile.password shouldBe ""
  }

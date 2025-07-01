package users_management.model

import org.scalatest.funsuite.AnyFunSuite
import org.scalatest.matchers.should.Matchers
import users_management.model.UserProfileBuilder
import org.scalatest.matchers.Matcher

class TestBuilder extends AnyFunSuite, Matchers:

  val validId = "123"
  val validName = "Mario Rossi"
  val validEmail = "mario.rossi@email.com"
  val validSettings: Set[String] = Set("dark_mode", "notifications")

  def profileMatcher(
      id: Option[String],
      name: String,
      email: String,
      settings: Set[String]
  ): Matcher[Object] =
    have(
      Symbol("id")(id),
      Symbol("name")(name),
      Symbol("email")(email),
      Symbol("settings")(settings)
    )

  test("build crea UserProfile con tutti i campi valorizzati") {
    val profile = UserProfileBuilder()
      .withId(validId)
      .withName(validName)
      .withEmail(validEmail)
      .withSettings(validSettings)
      .build()
    profile should profileMatcher(
      Some(validId),
      validName,
      validEmail,
      validSettings
    )
  }

  test("build crea UserProfile con valori di default se non impostati") {
    val profile = UserProfileBuilder().build()
    profile should profileMatcher(None, "", "", Set())
  }

  test("build crea UserProfile con solo alcuni campi impostati") {
    val profile = UserProfileBuilder()
      .withName(validName)
      .withEmail(validEmail)
      .build()
    profile should profileMatcher(None, validName, validEmail, Set())
  }

  test("withSettings sovrascrive i settings precedenti") {
    val profile = UserProfileBuilder()
      .withSettings(Set("a"))
      .withSettings(validSettings)
      .build()
    profile.settings shouldBe validSettings
  }

  test("withId accetta id vuoto") {
    val profile = UserProfileBuilder()
      .withId("")
      .build()
    profile.id shouldBe Some("")
  }

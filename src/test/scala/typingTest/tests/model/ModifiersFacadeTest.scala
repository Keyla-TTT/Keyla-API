package typingTest.tests.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ModifiersFacadeTest extends AnyFlatSpec with Matchers:
  val testWords: Seq[String] = Seq("Hello world", "test", "  spaces  ", "Mixed case")
  val nonStringInputs: Seq[Int | Boolean | (Double | Null)] = Seq(123, true, 3.14, null)

  "ModifiersFacade" should "apply uppercase transformation" in {
    val result = ModifiersFacade.uppercase(testWords)
    result shouldBe Seq("HELLO WORLD", "TEST", "  SPACES  ", "MIXED CASE")
  }

  it should "apply lowercase transformation" in {
    val result = ModifiersFacade.lowercase(testWords)
    result shouldBe Seq("hello world", "test", "  spaces  ", "mixed case")
  }

  it should "apply reverse transformation" in {
    val result = ModifiersFacade.reverse(testWords)
    result shouldBe Seq("dlrow olleH", "tset", "  secaps  ", "esac dexiM")
  }

  it should "apply capitalize transformation" in {
    val result = ModifiersFacade.capitalize(testWords)
    result shouldBe Seq("Hello world", "Test", "  spaces  ", "Mixed case")
  }

  it should "apply trim transformation" in {
    val result = ModifiersFacade.trim(testWords)
    result shouldBe Seq("Hello world", "test", "spaces", "Mixed case")
  }

  it should "apply noSpaces transformation" in {
    val result = ModifiersFacade.noSpaces(testWords)
    result shouldBe Seq("Helloworld", "test", "spaces", "Mixedcase")
  }

  it should "apply addPrefix transformation" in {
    val result = ModifiersFacade.addPrefix("pre_")(testWords)
    result shouldBe Seq(
      "pre_Hello world",
      "pre_test",
      "pre_  spaces  ",
      "pre_Mixed case"
    )
  }

  it should "apply addSuffix transformation" in {
    val result = ModifiersFacade.addSuffix("_suf")(testWords)
    result shouldBe Seq(
      "Hello world_suf",
      "test_suf",
      "  spaces  _suf",
      "Mixed case_suf"
    )
  }

  it should "handle non-string inputs" in {
    val result = ModifiersFacade.uppercase(nonStringInputs)
    result shouldBe Seq("123", "TRUE", "3.14")
  }

  it should "handle empty sequence" in {
    val result = ModifiersFacade.uppercase(Seq.empty)
    result shouldBe empty
  }

  it should "chain multiple modifiers" in {
    val result = ModifiersFacade.trim
      .andThen(ModifiersFacade.capitalize)
      .andThen(ModifiersFacade.addPrefix("pre_"))
      .apply(testWords)

    result shouldBe Seq(
      "pre_Hello world",
      "pre_Test",
      "pre_Spaces",
      "pre_Mixed case"
    )
  }

package typingTest.tests.model

import com.github.nscala_time.time.Imports.DateTime
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import typingTest.dictionary.model.Dictionary

class TypingTestSpec extends AnyFlatSpec with Matchers:

  "CompletedInfo" should "be created with default values" in {
    val info = CompletedInfo()
    info.completed shouldBe false
    info.completedDateTime shouldBe None
  }

  it should "be created with custom values" in {
    val now = DateTime.now()
    val info = CompletedInfo(true, Some(now))
    info.completed shouldBe true
    info.completedDateTime shouldBe Some(now)
  }

  "TypingTest" should "create a basic typing test correctly" in {
    val dictionary = Dictionary("test", "test", "/path/to/test.txt")
    val info = CompletedInfo()
    val words = Seq("word1", "word2")

    val test = TypingTest(
      sources = Set(dictionary),
      modifiers = Seq("uppercase"),
      info = info,
      words = words
    )

    test.sources shouldBe Set(dictionary)
    test.modifiers shouldBe Seq("uppercase")
    test.info shouldBe info
    test.words shouldBe words
  }

  it should "handle multiple sources and modifiers" in {
    val dict1 = Dictionary("dict1", "test", "/path/to/dict1.txt")
    val dict2 = Dictionary("dict2", "test", "/path/to/dict2.txt")

    val info = CompletedInfo(true, Some(DateTime.now()))
    val words = Seq("word1", "word2", "word3", "word4")

    val test = TypingTest(
      sources = Set(dict1, dict2),
      modifiers = Seq("uppercase", "reverse"),
      info = info,
      words = words
    )

    test.sources shouldBe Set(dict1, dict2)
    test.modifiers shouldBe Seq("uppercase", "reverse")
    test.info shouldBe info
    test.words shouldBe words
  }

  it should "handle empty sources and modifiers" in {
    val info = CompletedInfo()
    val words = Seq("word1", "word2")

    val test = TypingTest(
      sources = Set.empty,
      modifiers = Seq.empty,
      info = info,
      words = words
    )

    test.sources shouldBe Set.empty
    test.modifiers shouldBe Seq.empty
    test.info shouldBe info
    test.words shouldBe words
  }

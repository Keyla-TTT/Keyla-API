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

  "TypingTest" should "create a test with basic parameters" in {
    val dictionary = Dictionary("test", "/path/to/test.txt")
    val test = TypingTest(
      sources = Set(dictionary),
      modifiers = Seq("lowercase"),
      info = CompletedInfo(isCompleted = false, completedAt = None),
      words = Seq("word1", "word2", "word3")
    )

    test.sources should have size 1
    test.modifiers should contain("lowercase")
    test.words should contain theSameElementsAs Seq("word1", "word2", "word3")
    test.info.completed shouldBe false
  }

  it should "create a test with multiple sources" in {
    val dict1 = Dictionary("dict1", "/path/to/dict1.txt")
    val dict2 = Dictionary("dict2", "/path/to/dict2.txt")
    val test = TypingTest(
      sources = Set(dict1, dict2),
      modifiers = Seq("uppercase", "numbers"),
      info =
        CompletedInfo(isCompleted = true, completedAt = Some(DateTime.now())),
      words = Seq("word1", "word2", "word3", "word4")
    )

    test.sources should have size 2
    test.modifiers should contain allOf ("uppercase", "numbers")
    test.words should contain theSameElementsAs Seq(
      "word1",
      "word2",
      "word3",
      "word4"
    )
    test.info.completed shouldBe true
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

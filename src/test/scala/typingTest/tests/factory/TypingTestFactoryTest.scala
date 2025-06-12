package typingTest.tests.factory

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import typingTest.dictionary.loader.DictionaryLoader
import typingTest.dictionary.model.Dictionary
import typingTest.tests.model.{
  CompletedInfo,
  MergeOps,
  ModifiersFacade,
  NamedModifier,
  TestMerger,
  TypingTest
}

class MockDictionaryLoader extends DictionaryLoader:
  private val wordMap = Map(
    "dict1" -> Seq("word1", "word2", "word3"),
    "dict2" -> Seq("word4", "word5", "word6"),
    "empty" -> Seq.empty
  )

  override def loadWords(dictionary: Dictionary): Seq[String] =
    wordMap.getOrElse(dictionary.name, Seq.empty)

class TypingTestFactoryTest extends AnyFlatSpec with Matchers:
  val mockLoader = new MockDictionaryLoader()
  val dict1 = Dictionary("dict1", "test", "/path/to/dict1.txt")
  val dict2 = Dictionary("dict2", "test", "/path/to/dict2.txt")
  val emptyDict = Dictionary("empty", "test", "/path/to/empty.txt")

  "TypingTestFactory" should "create a test with a single source" in {
    val test = TypingTestFactory
      .create[String]()
      .useLoader(mockLoader)
      .useSource(dict1)
      .build

    test.sources shouldBe Set(dict1)
    test.words shouldBe Seq("word1", "word2", "word3")
    test.modifiers shouldBe empty
    test.info shouldBe CompletedInfo()
  }

  it should "create a test with multiple sources and a merger" in {
    val test = TypingTestFactory
      .create[String]()
      .useLoader(mockLoader)
      .useSource(dict1)
      .mergeWith(TestMerger.alternate)(dict2)
      .build

    test.sources shouldBe Set(dict1, dict2)
    test.words shouldBe Seq(
      "word1",
      "word4",
      "word2",
      "word5",
      "word3",
      "word6"
    )
    test.modifiers shouldBe empty
    test.info shouldBe CompletedInfo()
  }

  it should "create a test with uppercase modifier" in {
    val test = TypingTestFactory
      .create[String]()
      .useLoader(mockLoader)
      .useSource(dict1)
      .useModifier(ModifiersFacade.uppercase)
      .build

    test.sources shouldBe Set(dict1)
    test.words shouldBe Seq("WORD1", "WORD2", "WORD3")
    test.modifiers shouldBe Seq("uppercase")
    test.info shouldBe CompletedInfo()
  }

  it should "create a test with multiple modifiers" in {
    val test = TypingTestFactory
      .create[String]()
      .useLoader(mockLoader)
      .useSource(dict1)
      .useModifier(ModifiersFacade.uppercase)
      .useModifier(ModifiersFacade.reverse)
      .build

    test.sources shouldBe Set(dict1)
    test.words shouldBe Seq("1DROW", "2DROW", "3DROW")
    test.modifiers shouldBe Seq("uppercase", "reverse")
    test.info shouldBe CompletedInfo()
  }

  it should "create a test with complex modifier chain" in {
    val test = TypingTestFactory
      .create[String]()
      .useLoader(mockLoader)
      .useSource(dict1)
      .useModifier(ModifiersFacade.capitalize)
      .useModifier(ModifiersFacade.addPrefix("pre_"))
      .useModifier(ModifiersFacade.addSuffix("_suf"))
      .build

    test.sources shouldBe Set(dict1)
    test.words shouldBe Seq("pre_Word1_suf", "pre_Word2_suf", "pre_Word3_suf")
    test.modifiers shouldBe Seq("capitalize", "addPrefix", "addSuffix")
    test.info shouldBe CompletedInfo()
  }

  it should "handle empty dictionary" in {
    val test = TypingTestFactory
      .create[String]()
      .useLoader(mockLoader)
      .useSource(emptyDict)
      .build

    test.sources shouldBe Set(emptyDict)
    test.words shouldBe empty
    test.modifiers shouldBe empty
    test.info shouldBe CompletedInfo()
  }

  it should "throw exception when no loader is provided" in {
    an[IllegalArgumentException] should be thrownBy {
      TypingTestFactory
        .create[String]()
        .useSource(dict1)
        .build
    }
  }

  it should "throw exception when no source is provided" in {
    an[IllegalArgumentException] should be thrownBy {
      TypingTestFactory
        .create[String]()
        .useLoader(mockLoader)
        .build
    }
  }

  it should "throw exception when trying to add a second source with useSource" in {
    an[IllegalArgumentException] should be thrownBy {
      TypingTestFactory
        .create[String]()
        .useLoader(mockLoader)
        .useSource(dict1)
        .useSource(dict2)
        .build
    }
  }

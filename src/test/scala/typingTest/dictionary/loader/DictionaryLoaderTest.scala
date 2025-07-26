package typingTest.dictionary.loader

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import typingTest.dictionary.model.Dictionary
import typingTest.dictionary.model.DictionaryJson
import com.github.plokhotnyuk.jsoniter_scala.core.writeToArray

import java.io.File
import java.nio.file.Files

class DictionaryLoaderTest extends AnyFlatSpec with Matchers:
  "FileDictionaryLoader" should "load words from a dictionary file" in {
    val tempFile = File.createTempFile("test-dict", ".txt")
    Files.write(tempFile.toPath, "word1\nword2\nword3".getBytes)

    val dictionary = Dictionary("test-dict", tempFile.getAbsolutePath)
    val loader = new FileDictionaryLoader()

    val words = loader.loadWords(dictionary)
    words should contain theSameElementsAs Seq("word1", "word2", "word3")

    tempFile.delete()
  }

  it should "return empty sequence for non-existent file" in {
    val loader = new FileDictionaryLoader()
    val words = loader.loadWords(
      Dictionary("non-existent", "/path/to/non/existent/file.txt")
    )
    words shouldBe empty
  }

  it should "return empty sequence for empty file" in {
    val tempFile = File.createTempFile("empty-dict", ".txt")
    val loader = new FileDictionaryLoader()
    val words = loader.loadWords(
      Dictionary("empty-dict", tempFile.getAbsolutePath)
    )
    words shouldBe empty

    tempFile.delete()
  }

  it should "cache loaded words" in {
    val tempFile = File.createTempFile("cache-dict", ".txt")
    Files.write(tempFile.toPath, "cached1\ncached2".getBytes)

    val loader = new FileDictionaryLoader()
    val dictionary = Dictionary("cache-dict", tempFile.getAbsolutePath)

    val firstLoad = loader.loadWords(dictionary)
    val secondLoad = loader.loadWords(dictionary)

    firstLoad should contain theSameElementsAs Seq("cached1", "cached2")
    secondLoad should contain theSameElementsAs Seq("cached1", "cached2")

    tempFile.delete()
  }

  it should "handle lazy loading correctly" in {
    val tempFile = File.createTempFile("lazy-dict", ".txt")
    Files.write(tempFile.toPath, "lazy1\nlazy2\nlazy3".getBytes)

    val dictionary = Dictionary("lazy-dict", tempFile.getAbsolutePath)
    val loader = new FileDictionaryLoader()

    val words = loader.loadWords(dictionary)
    words should contain theSameElementsAs Seq("lazy1", "lazy2", "lazy3")

    tempFile.delete()
  }

  "JsonDictionaryLoader" should "load words from a JSON dictionary file" in {
    val tempFile = File.createTempFile("test-dict-json", ".json")
    val dictJson =
      DictionaryJson("test-dict-json", Seq("word1", "word2", "word3"))
    Files.write(tempFile.toPath, writeToArray(dictJson))

    val dictionary = Dictionary("test-dict-json", tempFile.getAbsolutePath)
    val loader = new JsonDictionaryLoader()

    val words = loader.loadWords(dictionary)
    words should contain theSameElementsAs Seq("word1", "word2", "word3")

    tempFile.delete()
  }

  it should "return empty sequence for non-existent JSON file" in {
    val loader = new JsonDictionaryLoader()
    val words = loader.loadWords(
      Dictionary("non-existent-json", "/path/to/non/existent/file.json")
    )
    words shouldBe empty
  }

  it should "return empty sequence for empty JSON file" in {
    val tempFile = File.createTempFile("empty-dict-json", ".json")
    val loader = new JsonDictionaryLoader()
    val words = loader.loadWords(
      Dictionary("empty-dict-json", tempFile.getAbsolutePath)
    )
    words shouldBe empty

    tempFile.delete()
  }

  it should "return empty sequence for malformed JSON file" in {
    val tempFile = File.createTempFile("malformed-dict-json", ".json")
    Files.write(tempFile.toPath, "{ invalid json }".getBytes)
    val loader = new JsonDictionaryLoader()
    val words = loader.loadWords(
      Dictionary("malformed-dict-json", tempFile.getAbsolutePath)
    )
    words shouldBe empty
    tempFile.delete()
  }

package typingTest.dictionary.loader

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import typingTest.dictionary.model.Dictionary
import java.io.File
import java.nio.file.Files

class DictionaryLoaderTest extends AnyFlatSpec with Matchers:
  val loader = new FileDictionaryLoader()
  
  "FileDictionaryLoader" should "load words from a valid dictionary file" in {
    val tempFile = File.createTempFile("test-dict", ".txt")
    try
      val words = List("word1", "word2", "word3")
      Files.write(tempFile.toPath, words.mkString("\n").getBytes)
      
      val dictionary = Dictionary("test-dict", "test", tempFile.getAbsolutePath)
      val loadedWords = loader.loadWords(dictionary)
      
      loadedWords should have size 3
      loadedWords should contain allOf("word1", "word2", "word3")
    finally
      tempFile.delete()
  }
  
  it should "return empty Vector for non-existent file" in {
    val dictionary = Dictionary("non-existent", "test", "/path/to/non/existent/file.txt")
    val loadedWords = loader.loadWords(dictionary)
    
    loadedWords shouldBe empty
  }
  
  it should "handle empty dictionary file" in {
    val tempFile = File.createTempFile("empty-dict", ".txt")
    try
      val dictionary = Dictionary("empty-dict", "test", tempFile.getAbsolutePath)
      val loadedWords = loader.loadWords(dictionary)
      
      loadedWords shouldBe empty
    finally
      tempFile.delete()
  }
  
  it should "cache loaded dictionaries" in {
    val tempFile = File.createTempFile("cache-dict", ".txt")
    try
      val words = List("word1", "word2", "word3")
      Files.write(tempFile.toPath, words.mkString("\n").getBytes)
      
      val dictionary = Dictionary("cache-dict", "test", tempFile.getAbsolutePath)
      
      val firstLoad = loader.loadWords(dictionary)
      val secondLoad = loader.loadWords(dictionary)
      
      firstLoad should be theSameInstanceAs secondLoad
      firstLoad should contain allOf("word1", "word2", "word3")
    finally
      tempFile.delete()
  }
  
  it should "load words lazily" in {
    val tempFile = File.createTempFile("lazy-dict", ".txt")
    try
      val words = List("word1", "word2", "word3")
      Files.write(tempFile.toPath, words.mkString("\n").getBytes)
      
      val dictionary = Dictionary("lazy-dict", "test", tempFile.getAbsolutePath)
      val loadedWords = loader.loadWords(dictionary)
      
      loadedWords.take(1).toList should contain only "word1"
      loadedWords.take(2).toList should contain allOf("word1", "word2")
    finally
      tempFile.delete()
  } 
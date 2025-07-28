package typingTest.dictionary.repository

import typingTest.dictionary.model.Dictionary

import java.io.File
import java.nio.file.{Files, Paths}
import scala.util.Using
import com.github.plokhotnyuk.jsoniter_scala.core.*
import com.github.plokhotnyuk.jsoniter_scala.macros.*

/** File-based implementation of DictionaryRepository
  *
  * @param baseDirectory
  *   The base directory for dictionary files
  */
class FileDictionaryRepository(
    baseDirectory: String
) extends DictionaryRepository:
  private val EXTENSIONS = Set(".json", ".txt")
  // Ensure the base directory exists
  private val basePath = Paths.get(baseDirectory)
  if !Files.exists(basePath) then Files.createDirectories(basePath)

  private def checkDirectoryExists(directory: String): Option[File] =
    val dir = new File(directory)
    if dir.exists() && dir.isDirectory
    then Some(dir)
    else None

  override def getAllDictionaries: Seq[Dictionary] =
    val check = checkDirectoryExists(baseDirectory)
    if check.isEmpty then return Seq.empty
    // Get all language directories
    getFolderDictionaries(check.get)

  override def getDictionaryByName(name: String): Option[Dictionary] =
    val check = checkDirectoryExists(baseDirectory)
    if check.isEmpty then return Option.empty

    EXTENSIONS
      .map(fileExtension =>
        val file = new File(s"${check.get.getPath}/$name$fileExtension")
        if file.exists() && file.isFile then
          Some(Dictionary(name, file.getAbsolutePath))
        else None
      )
      .find(_.isDefined)
      .flatten

  private def getJsonName(file: File): Option[String] =
    try
      val bytes = java.nio.file.Files.readAllBytes(file.toPath)
      val dict = com.github.plokhotnyuk.jsoniter_scala.core
        .readFromArray[typingTest.dictionary.model.DictionaryJson](bytes)
      Some(dict.name)
    catch case _: Throwable => Option.empty

  private def getFileName(file: File): Option[String] =
    EXTENSIONS
      .map(ext =>
        if file.getName.endsWith(ext) then Some(ext) else Option.empty
      )
      .find(_.isDefined)
      .flatten
      .flatMap {
        case ".json" => getJsonName(file)
        case ".txt"  => Some(file.getName.replace(".txt", ""))
        case _       => Option.empty
      } // If no extension found, return name as is

  private def getFolderDictionaries(languageDir: File) =
    println(s"Loading dictionaries from: ${languageDir.getAbsolutePath}")
    languageDir
      .listFiles()
      .filter(f =>
        f.isFile && EXTENSIONS
          .exists(fileExtension => f.getName.endsWith(fileExtension))
      )
      .map { file => (file, getFileName(file)) }
      .filter(_._2.isDefined)
      .map(x => Dictionary(x._2.get, x._1.getAbsolutePath))
      .toSeq

  override def close(): Unit =
    // No resources to close in this implementation
    ()

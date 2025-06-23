package typingTest.dictionary.repository

import typingTest.dictionary.model.Dictionary
import java.io.File
import java.nio.file.{Files, Paths}

/** File-based implementation of DictionaryRepository
  *
  * @param baseDirectory
  *   The base directory for dictionary files
  * @param fileExtension
  *   The file extension for dictionary files (default: .txt)
  */
class FileDictionaryRepository(
    baseDirectory: String = "src/main/resources/dictionaries",
    fileExtension: String = ".txt"
) extends DictionaryRepository:

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
    val languageDirs = check.get.listFiles().filter(_.isDirectory)
    // Get all dictionary files from all language directories
    languageDirs.flatMap(dir => getFolderDictionaries(dir.getName, dir)).toSeq

  override def getDictionariesByLanguage(language: String): Seq[Dictionary] =
    val check = checkDirectoryExists(s"$baseDirectory/$language")
    if check.isEmpty then return Seq.empty
    getFolderDictionaries(language, check.get)

  override def getDictionaryByName(name: String): Option[Dictionary] =
    val check = checkDirectoryExists(baseDirectory)
    if check.isEmpty then return Option.empty

    val languageDirs = check.get.listFiles().filter(_.isDirectory)
    // We take the first matching dictionary file found
    languageDirs.flatMap { langDir =>
      val language = langDir.getName
      val file = new File(s"${langDir.getPath}/$name$fileExtension")
      if file.exists() && file.isFile then
        Some(Dictionary(name, language, file.getAbsolutePath))
      else None
    }.headOption

  private def getFolderDictionaries(language: String, languageDir: File) =
    languageDir
      .listFiles()
      .filter(f => f.isFile && f.getName.endsWith(fileExtension))
      .map { file =>
        val name = file.getName.replace(fileExtension, "")
        Dictionary(name, language, file.getAbsolutePath)
      }
      .toSeq

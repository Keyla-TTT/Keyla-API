package typingTest.dictionary.model

/**
 * Represents metadata for a dictionary used in typing tests
 *
 * @param name The name of the dictionary (e.g., "italian-10k")
 * @param language The language of the dictionary (e.g., "italian")
 * @param filePath  path to the file containing the words
 */

case class Dictionary(
    name: String,
    language: String,
    filePath: String
)

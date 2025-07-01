package api.services

import typingTest.tests.model.{NamedModifier, ModifiersFacade}

/** Resolver for text transformation modifiers used in typing tests.
  *
  * This object provides a mapping between string modifier names (used in API
  * requests) and their corresponding NamedModifier implementations. It allows
  * typing tests to be customized with various text transformations such as case
  * changes, spacing modifications, and other text manipulations.
  *
  * =Available Modifiers=
  *
  * ==Case Transformation==
  *   - '''uppercase''': Convert all text to uppercase
  *   - '''lowercase''': Convert all text to lowercase
  *   - '''capitalize''': Capitalize the first letter of each word
  *
  * ==Text Processing==
  *   - '''reverse''': Reverse the order of characters in each word
  *   - '''trim''': Remove leading and trailing whitespace
  *   - '''removeSpaces/noSpaces''': Remove all spaces from the text
  *
  * =Usage in API=
  *
  * Modifiers are specified by name in typing test requests:
  * {{{
  * POST /api/tests
  * {
  *   "profileId": "user-123",
  *   "language": "english",
  *   "dictionaryName": "common_words",
  *   "wordCount": 50,
  *   "modifiers": ["uppercase", "noSpaces", "reverse"],
  *   "timeLimit": 60000
  * }
  * }}}
  *
  * =Modifier Chaining=
  *
  * Multiple modifiers can be applied to the same test text. They are applied in
  * the order specified in the request, creating a transformation pipeline:
  * {{{
  * Original: "Hello World"
  * + uppercase -> "HELLO WORLD"
  * + noSpaces -> "HELLOWORLD"
  * + reverse -> "DLROWOLLEH"
  * }}}
  *
  * @example
  *   {{{
  * // Check if a modifier is valid
  * val isValid = ModifierResolver.isValidModifier("uppercase") // true
  *
  * // Get all available modifiers
  * val available = ModifierResolver.getAvailableModifiers
  * // Set("uppercase", "lowercase", "reverse", "capitalize", "trim", "removeSpaces", "noSpaces")
  *
  * // Get a specific modifier for use
  * ModifierResolver.getModifier("uppercase") match {
  *   case Some(modifier) => // Apply modifier to test
  *   case None => // Handle unknown modifier
  * }
  *
  * // Create custom modifiers
  * val prefixModifier = ModifierResolver.createModifierWithPrefix(">>> ")
  * val suffixModifier = ModifierResolver.createModifierWithSuffix(" <<<")
  *   }}}
  */
object ModifierResolver:

  /** Internal mapping of modifier names to their implementations. This map
    * defines all available modifiers that can be requested via the API.
    *
    * Note: "removeSpaces" and "noSpaces" are aliases for the same modifier.
    */
  private val modifierMap: Map[String, NamedModifier[Any, String]] = Map(
    "uppercase" -> ModifiersFacade.uppercase,
    "lowercase" -> ModifiersFacade.lowercase,
    "reverse" -> ModifiersFacade.reverse,
    "capitalize" -> ModifiersFacade.capitalize,
    "trim" -> ModifiersFacade.trim,
    "removeSpaces" -> ModifiersFacade.noSpaces,
    "noSpaces" -> ModifiersFacade.noSpaces
  )

  /** Retrieves a modifier implementation by its name.
    *
    * @param name
    *   The modifier name (case-sensitive)
    * @return
    *   Some(modifier) if the name is recognized, None otherwise
    *
    * @example
    *   {{{
    * ModifierResolver.getModifier("uppercase") match {
    *   case Some(modifier) =>
    *     // Use modifier in test generation
    *     val transformedText = modifier.transform("hello world") // "HELLO WORLD"
    *   case None =>
    *     // Handle unknown modifier name
    *     throw InvalidModifier(name, getAvailableModifiers)
    * }
    *   }}}
    */
  def getModifier(name: String): Option[NamedModifier[Any, String]] =
    modifierMap.get(name)

  /** Returns the set of all available modifier names. Used for validation and
    * error messages when invalid modifiers are requested.
    *
    * @return
    *   Set of all recognized modifier names
    *
    * @example
    *   {{{
    * val available = ModifierResolver.getAvailableModifiers
    * println(s"Available modifiers: ${available.mkString(", ")}")
    * // Available modifiers: uppercase, lowercase, reverse, capitalize, trim, removeSpaces, noSpaces
    *   }}}
    */
  def getAvailableModifiers: Set[String] =
    modifierMap.keySet

  /** Checks if a modifier name is valid (recognized by the resolver).
    *
    * @param name
    *   The modifier name to validate
    * @return
    *   true if the modifier is available, false otherwise
    *
    * @example
    *   {{{
    * if (ModifierResolver.isValidModifier("uppercase")) {
    *   // Process the modifier
    * } else {
    *   // Return validation error
    * }
    *   }}}
    */
  def isValidModifier(name: String): Boolean =
    modifierMap.contains(name)

  /** Creates a custom modifier that adds a prefix to each word. This is useful
    * for creating dynamic modifiers not covered by the predefined set.
    *
    * @param prefix
    *   The string to prepend to each word
    * @return
    *   A NamedModifier that adds the specified prefix
    *
    * @example
    *   {{{
    * val modifier = ModifierResolver.createModifierWithPrefix(">> ")
    * val result = modifier.transform("hello world") // ">> hello >> world"
    *   }}}
    */
  def createModifierWithPrefix(prefix: String): NamedModifier[Any, String] =
    ModifiersFacade.addPrefix(prefix)

  /** Creates a custom modifier that adds a suffix to each word. Complements the
    * prefix modifier for full text decoration capabilities.
    *
    * @param suffix
    *   The string to append to each word
    * @return
    *   A NamedModifier that adds the specified suffix
    *
    * @example
    *   {{{
    * val modifier = ModifierResolver.createModifierWithSuffix(" <<")
    * val result = modifier.transform("hello world") // "hello << world <<"
    *   }}}
    */
  def createModifierWithSuffix(suffix: String): NamedModifier[Any, String] =
    ModifiersFacade.addSuffix(suffix)

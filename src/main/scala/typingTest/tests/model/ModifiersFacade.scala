package typingTest.tests.model

import scala.reflect.ClassTag

/** Facade object providing a collection of string transformation modifiers.
  * Each modifier is implemented as a NamedModifier that can transform sequences
  * of any type into sequences of strings.
  */
object ModifiersFacade:
  extension (modifierName: String)
    /** Creates a new NamedModifier using the <> operator syntax
      *
      * @param f
      *   The transformation function from Seq[Any] to Seq[O]
      * @tparam O
      *   The output element type
      * @return
      *   A new NamedModifier instance
      */
    def <>[O](f: Seq[Any] => Seq[O]): NamedModifier[Any, O] =
      NamedModifier[Any, O](modifierName)(f)

  /** Creates a modifier that filters elements to only include those of the
    * specified type
    *
    * @tparam O
    *   The type to filter for
    * @return
    *   A NamedModifier that filters elements by type
    */
  def onlyOfType[O: ClassTag]: NamedModifier[Any, O] =
    "identity" <> {
      _.flatMap {
        case s: O => Seq(s)
        case _    => Seq.empty
      }
    }

  /** Helper function that creates a string transformation function that handles
    * both strings and non-strings
    *
    * @param f
    *   The string transformation function to apply
    * @return
    *   A function that transforms any sequence into a sequence of strings
    */
  private def ofString(f: String => String): Seq[Any] => Seq[String] =
    _.filterNot(_ == null).map {
      case s: String => f(s)
      case other     => f(other.toString)
    }

  def limit(max: Int): NamedModifier[Any, String] =
    "limit" <> ofString(_.take(max))

  /** Creates a modifier that converts all strings to uppercase
    *
    * @return
    *   A NamedModifier that converts strings to uppercase
    */
  def uppercase: NamedModifier[Any, String] =
    "uppercase" <> ofString(_.toUpperCase)

  /** Creates a modifier that converts all strings to lowercase
    *
    * @return
    *   A NamedModifier that converts strings to lowercase
    */
  def lowercase: NamedModifier[Any, String] =
    "lowercase" <> ofString(_.toLowerCase)

  /** Creates a modifier that reverses all strings
    *
    * @return
    *   A NamedModifier that reverses strings
    */
  def reverse: NamedModifier[Any, String] =
    "reverse" <> ofString(_.reverse)

  /** Creates a modifier that capitalizes the first letter of each string
    *
    * @return
    *   A NamedModifier that capitalizes strings
    */
  def capitalize: NamedModifier[Any, String] =
    "capitalize" <> ofString(_.capitalize)

  /** Creates a modifier that removes leading and trailing whitespace from
    * strings
    *
    * @return
    *   A NamedModifier that trims strings
    */
  def trim: NamedModifier[Any, String] =
    "trim" <> ofString(_.trim)

  /** Creates a modifier that removes all whitespace from strings
    *
    * @return
    *   A NamedModifier that removes all spaces from strings
    */
  def noSpaces: NamedModifier[Any, String] =
    "removeSpaces" <> ofString(_.replaceAll("\\s+", ""))

  /** Creates a modifier that adds a prefix to all strings
    *
    * @param prefix
    *   The prefix to add to each string
    * @return
    *   A NamedModifier that adds the specified prefix
    */
  def addPrefix(prefix: String): NamedModifier[Any, String] =
    "addPrefix" <> ofString(prefix + _)

  /** Creates a modifier that adds a suffix to all strings
    *
    * @param suffix
    *   The suffix to add to each string
    * @return
    *   A NamedModifier that adds the specified suffix
    */
  def addSuffix(suffix: String): NamedModifier[Any, String] =
    "addSuffix" <> ofString(_ + suffix)

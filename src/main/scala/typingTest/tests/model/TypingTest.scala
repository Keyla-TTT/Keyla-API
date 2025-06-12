package typingTest.tests.model

import com.github.nscala_time.time.Imports.DateTime
import typingTest.dictionary.model.Dictionary

/** Base trait defining the typing context with associated types
  * @tparam Modifier
  *   The type of modifier used in the context
  * @tparam Source
  *   The source type for the typing test
  * @tparam Info
  *   The type of information associated with the test
  */
trait TypingContext:
  type Modifier
  type Source
  type Info

/** Trait defining basic information about a typing test completion. Provides
  * information about whether the test was completed and when
  */
trait CompletedInfo:
  /** Indicates whether the typing test has been completed
    * @return
    *   true if the test is completed, false otherwise
    */
  def completed: Boolean = completedDateTime.isDefined

  /** The date and time when the test was completed
    * @return
    *   Some(DateTime) if completed, None otherwise
    */
  def completedDateTime: Option[DateTime]
object CompletedInfo:
  private case class CompletedInfoImpl(
      override val completed: Boolean,
      override val completedDateTime: Option[DateTime]
  ) extends CompletedInfo

  def apply(
      isCompleted: Boolean,
      completedAt: Option[DateTime]
  ): CompletedInfo =
    CompletedInfoImpl(isCompleted, completedAt)
  def apply(): CompletedInfo = apply(false, None)

/** A named modifier that transforms a sequence of input elements to output
  * elements
  *
  * @tparam I
  *   The input element type
  * @tparam O
  *   The output element type
  */
trait NamedModifier[I, O] extends (Seq[I] => Seq[O]):
  /** The name of the modifier
    * @return
    *   String representing the modifier's name
    */
  def name: String

/** Companion object for NamedModifier providing factory methods */
object NamedModifier:
  /** Creates a new NamedModifier with the given name and transformation
    * function
    * @param modifierName
    *   The name of the modifier
    * @param f
    *   The transformation function from Seq[I] to Seq[O]
    * @tparam I
    *   The input element type
    * @tparam O
    *   The output element type
    * @return
    *   A new NamedModifier instance
    */
  def apply[I, O](modifierName: String)(
      f: Seq[I] => Seq[O]
  ): NamedModifier[I, O] =
    new NamedModifier[I, O]:
      override def name: String = modifierName
      override def apply(words: Seq[I]): Seq[O] = f(words)

/** The TypingTest trait is meant to describe a typing test, including its
  * sources, modifiers, and information about the test. There is no guarantee
  * that the words of the test are actually generated from the given sources,
  * with the given modifiers. This responsibility is left to its implementations
  * and usage.
  * @tparam I
  *   The type of elements in the test
  */
trait TypingTest[I] extends TypingContext:
  /** The set of sources used for the typing test
    * @return
    *   Set of Source instances
    */
  def sources: Set[Source]

  /** The set of modifiers applied to the test
    * @return
    *   Set of Modifier instances
    */
  def modifiers: Seq[Modifier]

  /** Information about the typing test
    * @return
    *   BasicTypingInfo instance
    */
  def info: Info

  /** The sequence of words in the test
    * @return
    *   Sequence of test elements
    */
  def words: Seq[I]

/** Concrete implementation of a typing test with string-based words
  * @param sources
  *   The set of dictionaries used as sources
  * @param modifiers
  *   The set of modifier names
  * @param info
  *   Information about the test completion
  * @param words
  *   The sequence of words in the test
  */
private case class BasicTypingTest[T](
    sources: Set[Dictionary],
    modifiers: Seq[String],
    info: CompletedInfo,
    words: Seq[T]
) extends TypingTest[T]:
  override type Info = CompletedInfo
  override type Modifier = String
  override type Source = Dictionary

object TypingTest:
  /** Creates a new BasicTypingTest instance
    * @param sources
    *   The set of dictionaries used as sources
    * @param modifiers
    *   The set of modifier names
    * @param info
    *   Information about the test completion
    * @param words
    *   The sequence of words in the test
    * @return
    *   A new BasicTypingTest instance
    */
  def apply[T](
      sources: Set[Dictionary],
      modifiers: Seq[String],
      info: CompletedInfo,
      words: Seq[T]
  ): TypingTest[T] = BasicTypingTest(sources, modifiers, info, words)

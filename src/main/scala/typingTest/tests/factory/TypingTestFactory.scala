package typingTest.tests.factory

import typingTest.dictionary.loader.DictionaryLoader
import typingTest.dictionary.model.Dictionary
import typingTest.tests.model.ModifiersFacade.onlyOfType
import typingTest.tests.model.*
import typingTest.tests.model.MergeOps

import scala.reflect.ClassTag
import scala.util.Random

private case class TestBuilder[O](
    private val loader: Option[DictionaryLoader] = None,
    private val mergers: Seq[MergeOps[Any]] = Seq.empty,
    private val sources: Seq[Dictionary] = Seq.empty,
    private val modifiers: Seq[NamedModifier[Any, O]] = Seq.empty
)
object TestBuilder:
  extension [O: ClassTag](builder: TestBuilder[O])
    def useLoader(loader: DictionaryLoader): TestBuilder[O] =
      builder.copy(loader = Some(loader))
    def mergeWith(merger: MergeOps[Any])(source: Dictionary): TestBuilder[O] =
      builder.copy(
        mergers = builder.mergers :+ merger,
        sources = builder.sources :+ source
      )
    def useModifiers(modifiers: Seq[NamedModifier[Any, O]]): TestBuilder[O] =
      builder.copy(modifiers = builder.modifiers ++ modifiers)
    def mergeMultiple(
        sources: Seq[(Dictionary, MergeOps[Any])]
    ): TestBuilder[O] =
      require(
        builder.sources.nonEmpty,
        "At least one source must be provided for merging"
      )
      val (srcs, merges) = sources.unzip
      builder.copy(
        mergers = builder.mergers ++ merges,
        sources = builder.sources ++ srcs
      )
    def useSource(
        source: Dictionary,
        randomized: Boolean = false
    ): TestBuilder[O] =
      require(
        builder.sources.isEmpty,
        "Source already exists, cannot add a new one"
      )
      builder.copy(
        sources = Seq(source),
        mergers =
          if randomized
          then Seq(TestMerger.randomMix)
          else Seq(TestMerger.concatenate)
      )
    def useModifier(modifier: NamedModifier[Any, O]): TestBuilder[O] =
      builder.copy(modifiers = builder.modifiers :+ modifier)
    def build: TypingTest[O] & DefaultContext =
      require(builder.loader.isDefined, "Loader must be defined")
      require(builder.sources.nonEmpty, "At least one source must be defined")
      val modifiers =
        if builder.modifiers.isEmpty then Seq(onlyOfType[O])
        else builder.modifiers
      // loads and shuffle by default
      val words = builder.sources.map(builder.loader.get.loadWords)
      val zipped = words.zip(builder.mergers)
      val mergedWords = zipped.foldLeft(Seq())((acc, mergeOps) =>
        mergeOps._2.merge(acc, mergeOps._1)
      )
      val modifiedWords = modifiers.tail
        .foldLeft(modifiers.head.apply(mergedWords))((acc, modifier) =>
          modifier.apply(acc)
        )
      TypingTest(
        sources = builder.sources.toSet,
        words = modifiedWords,
        modifiers = builder.modifiers.map(_.name),
        info = CompletedInfo()
      )

object TypingTestFactory:
  def create[O](): TestBuilder[O] = TestBuilder[O](
    loader = None,
    mergers = Seq.empty,
    sources = Seq.empty,
    modifiers = Seq.empty
  )

  extension (test: TypingTest[String] & DefaultContext)
    def copy(
        words: Seq[String] = test.words,
        info: CompletedInfo = test.info,
        sources: Set[Dictionary] = test.sources,
        modifiers: Seq[String] = test.modifiers
    ): TypingTest[String] & DefaultContext =
      TypingTest(
        sources = sources,
        words = words,
        modifiers = modifiers,
        info = info
      )

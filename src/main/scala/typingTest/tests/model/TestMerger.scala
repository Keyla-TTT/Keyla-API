package typingTest.tests.model

import scala.util.Random

trait MergeOps[T]:
  /** Merges two sequences of elements into one
    * @param s1
    *   First sequence of elements
    * @param s2
    *   Second sequence of elements
    * @return
    *   Merged sequence containing elements from both input sequences
    */
  def merge(s1: Seq[T], s2: Seq[T]): Seq[T]

object MergeOps:
  def apply[T](f: (Seq[T], Seq[T]) => Seq[T]): MergeOps[T] =
    (s1: Seq[T], s2: Seq[T]) => f(s1, s2)

object TestMerger:
  /** Creates a merger that alternates elements from both sequences
    * @return
    *   A merger that produces a sequence with alternating elements from both
    *   input sequences, with remaining elements from the longer sequence
    *   appended at the end
    */
  def alternate: MergeOps[Any] = MergeOps((s1, s2) =>
    val minLength = math.min(s1.size, s2.size)
    val alternating = (0 until minLength).flatMap(i => Seq(s1(i), s2(i)))
    val remaining =
      if s1.size > s2.size then s1.drop(minLength)
      else s2.drop(minLength)
    alternating ++ remaining
  )

  /** Creates a merger that randomly shuffles elements from both sequences
    * @return
    *   A merger that produces a sequence with randomly mixed elements from both
    *   input sequences
    */
  def randomMix: MergeOps[Any] = MergeOps((s1, s2) => Random.shuffle(s1 ++ s2))

  /** Creates a merger that concatenates two sequences
    * @return
    *   A merger that produces a sequence with elements from the first sequence
    *   followed by elements from the second sequence
    */
  def concatenate: MergeOps[Any] = MergeOps(_ ++ _)

  /** Creates a merger that inserts elements from the second sequence at random
    * positions in the first sequence
    * @return
    *   A merger that produces a sequence with elements from the second sequence
    *   inserted at random positions in the first sequence
    */
  def random: MergeOps[Any] = MergeOps((s1, s2) =>
    val positions = Random.shuffle(0 to s1.size).take(s2.size)
    val result = s1.toBuffer
    positions.sorted.zip(s2).reverse.foreach { case (pos, word) =>
      result.insert(pos, word)
    }
    result.toSeq
  )

  /** Creates a merger that probabilistically selects elements from both
    * sequences
    * @param size
    *   The desired size of the resulting sequence
    * @param probability1
    *   Probability of selecting an element from the first sequence (0.0 to 1.0)
    * @return
    *   A merger that produces a sequence of the specified size by
    *   probabilistically selecting elements from both input sequences
    * @throws java.lang.IllegalArgumentException
    *   if probability1 is not between 0.0 and 1.0 or if size is not positive
    * @throws java.lang.IllegalStateException
    *   if there are not enough elements in either sequence to satisfy the size
    *   requirement
    */
  def probabilistic(size: Int, probability1: Double): MergeOps[Any] =
    require(
      probability1 >= 0.0 && probability1 <= 1.0,
      "Probability must be between 0.0 and 1.0"
    )
    require(size > 0, "Size must be positive")

    MergeOps((s1, s2) =>
      if s1.isEmpty && s2.isEmpty then
        throw new IllegalStateException(
          "Not enough elements in either sequence to satisfy size requirement"
        )

      val words1 = Random.shuffle(s1).toBuffer
      val words2 = Random.shuffle(s2).toBuffer

      (0 until size).map { _ =>
        if Random.nextDouble() < probability1 && words1.nonEmpty then
          words1.remove(0)
        else if words2.nonEmpty then words2.remove(0)
        else if words1.nonEmpty then words1.remove(0)
        else
          throw new IllegalStateException(
            "Not enough elements in either sequence to satisfy size requirement"
          )
      }
    )

  /** Creates a merger that interleaves elements from both sequences in chunks
    * @param chunkSize
    *   The size of chunks to interleave
    * @return
    *   A merger that produces a sequence by interleaving chunks of elements
    *   from both input sequences
    */
  def interleaveChunks(chunkSize: Int): MergeOps[Any] = MergeOps((s1, s2) =>
    val chunks1 = s1.grouped(chunkSize).toSeq
    val chunks2 = s2.grouped(chunkSize).toSeq
    chunks1
      .zipAll(chunks2, Seq.empty, Seq.empty)
      .flatMap { case (c1, c2) => c1 ++ c2 }
  )

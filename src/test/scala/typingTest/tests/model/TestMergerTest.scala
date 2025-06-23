package typingTest.tests.model

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers
import typingTest.tests.model.TestMerger

class TestMergerTest extends AnyFlatSpec with Matchers:
  val seq1: List[String] = List("one", "two", "three")
  val seq2: List[String] = List("uno", "due", "tre", "quattro")

  "TestMerger.alternate" should "create a generic merger that alternates elements" in {
    val merger = TestMerger.alternate
    val result = merger.merge(seq1, seq2)
    result shouldBe List("one", "uno", "two", "due", "three", "tre", "quattro")

    val result2 = merger.merge(seq2, seq1)
    result2 shouldBe List("uno", "one", "due", "two", "tre", "three", "quattro")
  }

  "TestMerger.randomMix" should "create a generic merger that randomly mixes elements" in {
    val merger = TestMerger.randomMix
    val result = merger.merge(seq1, seq2)
    result should have size (seq1.size + seq2.size)
    result.toSet shouldBe (seq1 ++ seq2).toSet
  }

  "TestMerger.concatenate" should "create a generic merger that concatenates sequences" in {
    val merger = TestMerger.concatenate
    val result = merger.merge(seq1, seq2)
    result shouldBe seq1 ++ seq2
  }

  "TestMerger.insertRandom" should "create a generic merger that inserts elements at random positions" in {
    val merger = TestMerger.random
    val result = merger.merge(seq1, seq2)
    result should have size (seq1.size + seq2.size)
    result.toSet shouldBe (seq1 ++ seq2).toSet
  }

  "TestMerger.probabilistic" should "create a generic merger that merges based on probability" in {
    val dict1 = List.fill(10000)("a")
    val dict2 = List.fill(10000)("b")
    val targetSize = 1000
    val probability1 = 0.8
    val tolerance = 0.07 // 7% tolerance

    val merger = TestMerger.probabilistic(targetSize, probability1)
    val result = merger.merge(dict1, dict2)

    result should have size targetSize
    val countA = result.count(_ == "a")
    val actualProbability = countA.toDouble / targetSize

    actualProbability should be(probability1 +- tolerance)
    result should contain("a")
    result should contain("b")
  }

  "TestMerger.interleaveChunks" should "create a generic merger that interleaves in chunks" in {
    val merger = TestMerger.interleaveChunks(2)
    val result = merger.merge(seq1, seq2)
    result should have size (seq1.size + seq2.size)
    result.toSet shouldBe (seq1 ++ seq2).toSet
  }

  "Generic mergers" should "work with different types" in {
    val intSeq1 = List(1, 2, 3)
    val intSeq2 = List(4, 5, 6)

    val stringMerger = TestMerger.alternate
    val intMerger = TestMerger.alternate

    stringMerger.merge(seq1, seq2) shouldBe List(
      "one",
      "uno",
      "two",
      "due",
      "three",
      "tre",
      "quattro"
    )
    intMerger.merge(intSeq1, intSeq2) shouldBe List(1, 4, 2, 5, 3, 6)
  }

  "Mergers" should "handle empty sequences" in {
    val empty = List.empty
    val merger = TestMerger.alternate
    merger.merge(empty, seq2) shouldBe seq2
    merger.merge(seq1, empty) shouldBe seq1
    merger.merge(empty, empty) shouldBe empty
  }

  "TestMerger.probabilistic" should "throw exception for invalid probability" in {
    an[IllegalArgumentException] should be thrownBy {
      TestMerger.probabilistic(5, 1.5)
    }
  }

  it should "throw exception for invalid size" in {
    an[IllegalArgumentException] should be thrownBy {
      TestMerger.probabilistic(0, 0.5)
    }
  }

  it should "throw exception when not enough elements for probabilistic merge" in {
    an[IllegalStateException] should be thrownBy {
      TestMerger.probabilistic(10, 0.5).merge(List("one"), List("uno"))
    }
  }

package preCommitTest

import org.scalatest.funsuite.AnyFunSuite

class PreCommitTest extends AnyFunSuite:

  // Codice volutamente malformattato per testare scalafmt
  val nonFormattedCode =
    "class BadFormatting{def method(   x:Int,y:     Int)=x+y}"

  // Codice che dovrebbe generare warning di scalafix
  @deprecated var unusedVariable = 42 // warning: var potrebbe essere val

  test("verify code formatting") {
    // Il codice seguente è malformattato e dovrebbe essere corretto da scalafmt
    def badlyFormattedMethod(a: Int, b: Int) =
      a + b

    // Questo test passerà sempre, serve solo per vedere se scalafmt lo formatta
    assert(badlyFormattedMethod(1, 2) === 3)
  }

  test("verify scalafix rules") {
    // Questo codice dovrebbe generare warning di scalafix
    var mutableVar = 10
    mutableVar += 1

    assert(mutableVar === 11)
  }

import cats.effect.unsafe.implicits.global

object Launcher:

  def main(args: Array[String]): Unit =

    Main.run(args.toList).unsafeRunSync()

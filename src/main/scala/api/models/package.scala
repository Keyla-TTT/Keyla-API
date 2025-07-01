package api

import api.models.AppError
import cats.data.EitherT
import cats.effect.IO

package object models:

  type AppResult[A] = EitherT[IO, AppError, A]

  object AppResult:
    def pure[A](value: A): AppResult[A] =
      EitherT.pure[IO, AppError](value)

    def liftF[A](io: IO[A]): AppResult[A] =
      EitherT.liftF[IO, AppError, A](io)

    def fromEither[A](either: Either[AppError, A]): AppResult[A] =
      EitherT.fromEither[IO](either)

    def raiseError[A](error: AppError): AppResult[A] =
      EitherT.leftT[IO, A](error)

    def attemptBlocking[A](io: IO[A])(
        errorMapper: Throwable => AppError
    ): AppResult[A] =
      EitherT(io.attempt.map(_.left.map(errorMapper)))

    def fromOption[A](opt: Option[A], error: => AppError): AppResult[A] =
      EitherT.fromOption[IO](opt, error)

    def fromOptionF[A](optIO: IO[Option[A]], error: => AppError): AppResult[A] =
      EitherT(optIO.map(_.toRight(error)))

  extension [A](appResult: AppResult[A])
    def toIO: IO[Either[AppError, A]] = appResult.value

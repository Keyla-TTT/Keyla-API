package analytics.service

import analytics.model.TypingStatistics

import scala.concurrent.{ExecutionContext, Future}

class TypingStatisticsServiceImpl(implicit ec: ExecutionContext)
    extends TypingStatisticsService:

  def calculateAndSaveStatistics(
      userId: String,
      typedText: String,
      targetText: String,
      timeInSeconds: Double
  ): Future[TypingStatistics] =
    // Calcolo WPM (Words Per Minute)
    val words = typedText.split(" ").length
    val minutes = timeInSeconds / 60
    val wpm = words / minutes

    // Calcolo accuratezza
    val correctChars = typedText.zip(targetText).count { case (a, b) => a == b }
    val accuracy = (correctChars.toDouble / targetText.length) * 100

    val stats = TypingStatistics(
      userId = userId,
      wpm = wpm,
      accuracy = accuracy,
      timestamp = System.currentTimeMillis()
    )

    // Qui implementare la logica per salvare su DB
    saveToDatabase(stats)

  private def saveToDatabase(
      stats: TypingStatistics
  ): Future[TypingStatistics] =
    // Implementare la logica di salvataggio su DB

    Future.successful(stats) // placeholder

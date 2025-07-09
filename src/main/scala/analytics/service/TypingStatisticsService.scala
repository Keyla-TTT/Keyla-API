package analytics.service

import analytics.model.TypingStatistics

trait TypingStatisticsService:
  /** Calculates and saves typing statistics based on user input.
    *
    * @param userId
    *   The unique identifier of the user
    * @param typedText
    *   The text typed by the user
    * @param targetText
    *   The target text that the user was supposed to type
    * @param timeInSeconds
    *   The time taken by the user to type the text, in seconds
    * @return
    *   A future containing the calculated typing statistics
    */
  def calculateAndSaveStatistics(
      userId: String,
      typedText: String,
      targetText: String,
      timeInSeconds: Double
  ): TypingStatistics

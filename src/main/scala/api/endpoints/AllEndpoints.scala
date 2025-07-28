package api.endpoints

import api.models.common.ErrorResponse
import sttp.tapir.*

object AllEndpoints:
  export api.endpoints.users.UsersEndpoints.{
    createProfile,
    getAllProfiles,
    getProfileById,
    updateProfile,
    deleteProfile
  }
  export api.endpoints.typingtest.TypingTestEndpoints.{
    requestTest,
    getTestById,
    getTestsByProfileId,
    getLastTest,
    submitTestResults,
    getAllDictionaries,
    getAllModifiers,
    getAllMergers
  }
  export api.endpoints.stats.StatsEndpoints.{
    saveStatistics,
    getAllProfileStatistics
  }
  export api.endpoints.analytics.AnalyticsEndpoints.getUserAnalytics
  export api.endpoints.config.ConfigEndpoints.{
    getAllConfigEntries,
    getConfigEntry,
    updateConfigEntry,
    getCurrentConfig,
    reloadConfig,
    resetConfigToDefaults
  }

  val getAllEndpoints: List[Endpoint[?, ?, ErrorResponse, ?, ?]] = List(
    createProfile,
    getAllProfiles,
    getProfileById,
    updateProfile,
    deleteProfile,
    requestTest,
    getTestById,
    getTestsByProfileId,
    getLastTest,
    submitTestResults,
    getAllDictionaries,
    getAllModifiers,
    getAllMergers,
    saveStatistics,
    getAllProfileStatistics,
    getUserAnalytics,
    getAllConfigEntries,
    getCurrentConfig,
    reloadConfig,
    resetConfigToDefaults,
    getConfigEntry,
    updateConfigEntry
  )

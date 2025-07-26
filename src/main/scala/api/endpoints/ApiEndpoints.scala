package api.endpoints

import api.endpoints.analytics.AnalyticsEndpoints
import api.endpoints.config.ConfigEndpoints
import api.endpoints.stats.StatsEndpoints
import api.endpoints.typingtest.TypingTestEndpoints
import api.endpoints.users.UsersEndpoints

object ApiEndpoints:
  export api.endpoints.users.UsersEndpoints.*
  export api.endpoints.typingtest.TypingTestEndpoints.*
  export api.endpoints.stats.StatsEndpoints.*
  export api.endpoints.analytics.AnalyticsEndpoints.*
  export api.endpoints.config.ConfigEndpoints.*

package org.joinmastodon.android.api.requests.notifications

import org.joinmastodon.android.api.AllFieldsAreRequired
import org.joinmastodon.android.api.ApiUtils.enumSetToStrings
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.api.ObjectValidationException
import org.joinmastodon.android.api.requests.notifications.GetNotificationsV2.GroupedNotificationsResults
import org.joinmastodon.android.model.Account
import org.joinmastodon.android.model.BaseModel
import org.joinmastodon.android.model.NotificationGroup
import org.joinmastodon.android.model.NotificationType
import org.joinmastodon.android.model.Status
import java.util.EnumSet

class GetNotificationsV2 @JvmOverloads constructor(
  maxID: String?,
  limit: Int,
  includeTypes: EnumSet<NotificationType>?,
  groupedTypes: EnumSet<NotificationType>?,
  onlyAccountID: String? = null
) : MastodonAPIRequest<GroupedNotificationsResults>(
    method = HttpMethod.GET,
    path = "/notifications",
    respClass = GroupedNotificationsResults::class.java
  ) {
  init {
    if (maxID != null) addQueryParameter("max_id", maxID)
    if (limit > 0) addQueryParameter("limit", limit.toString())

    includeTypes?.let { types ->

      enumSetToStrings(types, NotificationType::class.java)
        .forEach { typeName ->
          addQueryParameter("types[]", typeName)
        }


      enumSetToStrings(EnumSet.complementOf(types), NotificationType::class.java)
        .forEach { excludedTypeName ->
          addQueryParameter("exclude_types[]", excludedTypeName)
        }

    }

    groupedTypes?.let { types ->

      enumSetToStrings(types, NotificationType::class.java)
        .forEach { typeNames ->
          addQueryParameter("grouped_types[]", typeNames)
        }

    }


    if (!onlyAccountID.isNullOrEmpty()) {
      addQueryParameter("account_id", onlyAccountID)
    }

    removeUnsupportedItems = true
  }

  override fun getPathPrefix() = "/api/v2"


  @AllFieldsAreRequired
  class GroupedNotificationsResults : BaseModel() {
    @JvmField
    @JvmSuppressWildcards
    var accounts: List<Account> = emptyList()

    @JvmField
    @JvmSuppressWildcards
    var statuses: List<Status> = emptyList()

    @JvmField
    @JvmSuppressWildcards
    var notificationGroups: List<NotificationGroup> = emptyList()

    /**
    * suppression added, as the callback was capturing a generic, and not understanding the correct value
    * due to kotlin's automatic wildcard assignment
   */

    @Throws(ObjectValidationException::class)
    override fun postprocess() {
      super.postprocess()
      accounts.forEach { it.postprocess() }
      statuses.forEach { it.postprocess() }
      notificationGroups.forEach { it.postprocess() }
    }
  }
}

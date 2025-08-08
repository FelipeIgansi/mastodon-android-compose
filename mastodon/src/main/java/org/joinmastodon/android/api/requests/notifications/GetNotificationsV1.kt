package org.joinmastodon.android.api.requests.notifications

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.ApiUtils.enumSetToStrings
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Notification
import org.joinmastodon.android.model.NotificationType
import java.util.EnumSet

class GetNotificationsV1 @JvmOverloads constructor(
    maxID: String?,
    limit: Int,
    includeTypes: EnumSet<NotificationType>?,
    onlyAccountID: String? = null
) : MastodonAPIRequest< MutableList<Notification>>(
    method = HttpMethod.GET,
    path = "/notifications",
    respTypeToken = object : TypeToken<MutableList<Notification>>() {}
) {
    /**
    suppression added, as the callback was capturing a generic, and not understanding the correct value
    due to kotlin's automatic wildcard assignment*/
    init {
        if (maxID != null) addQueryParameter("max_id", maxID)
        if (limit > 0) addQueryParameter("limit", limit.toString())

        includeTypes?.let { types ->
            enumSetToStrings(
                types,
                NotificationType::class.java
            ).forEach { typeName ->
                addQueryParameter("types[]", typeName)
            }

            enumSetToStrings(
                EnumSet.complementOf(types),
                NotificationType::class.java
            ).forEach { excludedTypeName ->
                addQueryParameter("exclude_types[]", excludedTypeName)
            }
        }

        if (!onlyAccountID.isNullOrEmpty()) {
            addQueryParameter("account_id", onlyAccountID)
        }

        removeUnsupportedItems = true
    }
}

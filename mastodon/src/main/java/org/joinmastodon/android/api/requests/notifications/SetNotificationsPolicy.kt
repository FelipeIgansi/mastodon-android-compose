package org.joinmastodon.android.api.requests.notifications

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.NotificationsPolicy

class SetNotificationsPolicy(policy: NotificationsPolicy) :
    MastodonAPIRequest<NotificationsPolicy>(
        method = HttpMethod.PUT,
        path = "/notifications/policy",
        respClass = NotificationsPolicy::class.java
    ) {
    init {
        setRequestBody(policy)
    }
}

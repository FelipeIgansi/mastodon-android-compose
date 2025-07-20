package org.joinmastodon.android.api.requests.notifications

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.NotificationsPolicy

class GetNotificationsPolicy :
    MastodonAPIRequest<NotificationsPolicy>(
        method = HttpMethod.GET,
        path = "/notifications/policy",
        respClass = NotificationsPolicy::class.java
    )

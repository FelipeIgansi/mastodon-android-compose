package org.joinmastodon.android.api.requests.notifications

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Notification

class GetNotificationByID(id: String) :
    MastodonAPIRequest<Notification>(
        method = HttpMethod.GET,
        path = "/notifications/$id",
        respClass = Notification::class.java
    )

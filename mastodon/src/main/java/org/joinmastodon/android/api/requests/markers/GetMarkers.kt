package org.joinmastodon.android.api.requests.markers

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.TimelineMarkers

class GetMarkers : MastodonAPIRequest<TimelineMarkers>(
    method = HttpMethod.GET,
    path = "/markers",
    respClass = TimelineMarkers::class.java
) {
    init {
        addQueryParameter("timeline[]", "home")
        addQueryParameter("timeline[]", "notifications")
    }
}

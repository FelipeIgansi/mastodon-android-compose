package org.joinmastodon.android.api.requests.markers

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.api.gson.JsonObjectBuilder
import org.joinmastodon.android.model.TimelineMarkers

class SaveMarkers(
    lastSeenHomePostID: String?,
    lastSeenNotificationID: String?
) : MastodonAPIRequest<TimelineMarkers>(
        method = HttpMethod.POST,
        path = "/markers",
        respClass = TimelineMarkers::class.java
    ) {
    init {
        val builder = JsonObjectBuilder()

        lastSeenHomePostID?.let {
            builder.add(
                key = "home",
                el = JsonObjectBuilder().add("last_read_id", it)
            )
        }

        lastSeenNotificationID?.let {
            builder.add(
                key = "notifications",
                el = JsonObjectBuilder().add("last_read_id", it)
            )
        }

        setRequestBody(builder.build())
    }
}

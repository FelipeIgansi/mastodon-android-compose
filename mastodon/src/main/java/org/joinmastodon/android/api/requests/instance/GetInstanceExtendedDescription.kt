package org.joinmastodon.android.api.requests.instance

import org.joinmastodon.android.api.MastodonAPIRequest
import java.time.Instant

class GetInstanceExtendedDescription :
    MastodonAPIRequest<GetInstanceExtendedDescription.Response>(
        method = HttpMethod.GET,
        path = "/instance/extended_description",
        respClass = Response::class.java
    ) {
    class Response {
        @JvmField
        var updatedAt: Instant? = null

        @JvmField
        var content: String? = null
    }
}

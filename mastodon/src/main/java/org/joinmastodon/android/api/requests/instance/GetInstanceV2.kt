package org.joinmastodon.android.api.requests.instance

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.InstanceV2

class GetInstanceV2 : MastodonAPIRequest<InstanceV2>(
    method = HttpMethod.GET,
    path = "/instance",
    respClass = InstanceV2::class.java
) {
    override fun getPathPrefix() = "/api/v2"
}

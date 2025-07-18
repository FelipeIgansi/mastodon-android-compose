package org.joinmastodon.android.api.requests.instance

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.InstanceV1

class GetInstanceV1 : MastodonAPIRequest<InstanceV1>(
    method = HttpMethod.GET,
    path = "/instance",
    respClass = InstanceV1::class.java
)

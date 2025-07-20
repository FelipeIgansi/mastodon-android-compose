package org.joinmastodon.android.api.requests.statuses

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.StatusContext

class GetStatusContext(id: String) :
  MastodonAPIRequest<StatusContext>(
    method = HttpMethod.GET,
    path = "/statuses/$id/context",
    respClass = StatusContext::class.java
  )

package org.joinmastodon.android.api.requests.statuses

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Status

class GetStatusByID(id: String) :
  MastodonAPIRequest<Status>(
    method = HttpMethod.GET,
    path = "/statuses/$id",
    respClass = Status::class.java
  )

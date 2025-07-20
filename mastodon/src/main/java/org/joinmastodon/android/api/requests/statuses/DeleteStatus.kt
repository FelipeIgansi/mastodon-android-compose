package org.joinmastodon.android.api.requests.statuses

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Status

class DeleteStatus(id: String) : MastodonAPIRequest<Status>(
  method = HttpMethod.DELETE,
  path = "/statuses/$id",
  respClass = Status::class.java
)

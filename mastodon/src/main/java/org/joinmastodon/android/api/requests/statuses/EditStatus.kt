package org.joinmastodon.android.api.requests.statuses

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.api.requests.statuses.CreateStatus.Request
import org.joinmastodon.android.model.Status

class EditStatus(req: Request, id: String) :
  MastodonAPIRequest<Status>(
    method = HttpMethod.PUT,
    path = "/statuses/$id",
    respClass = Status::class.java
  ) {
  init {
    setRequestBody(req)
  }
}

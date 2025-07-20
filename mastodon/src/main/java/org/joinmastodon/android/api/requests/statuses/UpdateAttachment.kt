package org.joinmastodon.android.api.requests.statuses

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Attachment

class UpdateAttachment(id: String, description: String) :
  MastodonAPIRequest<Attachment>(
    method = HttpMethod.PUT,
    path = "/media/$id",
    respClass = Attachment::class.java
  ) {
  init {
    setRequestBody(Body(description))
  }

  private data class Body(val description: String)
}

package org.joinmastodon.android.api.requests.lists

import org.joinmastodon.android.api.ResultlessMastodonAPIRequest

class DeleteList(id: String?) : ResultlessMastodonAPIRequest(
  method = HttpMethod.DELETE,
  path = "/lists/$id"
)

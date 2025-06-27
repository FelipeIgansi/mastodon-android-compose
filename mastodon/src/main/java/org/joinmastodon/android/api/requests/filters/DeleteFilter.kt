package org.joinmastodon.android.api.requests.filters

import org.joinmastodon.android.api.ResultlessMastodonAPIRequest

class DeleteFilter(id: String?) :
  ResultlessMastodonAPIRequest(
    method = HttpMethod.DELETE,
    path = "/filters/$id"
  ) {
  override fun getPathPrefix(): String {
    return "/api/v2"
  }
}

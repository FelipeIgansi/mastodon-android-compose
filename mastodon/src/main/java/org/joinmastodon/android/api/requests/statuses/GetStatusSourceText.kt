package org.joinmastodon.android.api.requests.statuses

import org.joinmastodon.android.api.AllFieldsAreRequired
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.BaseModel

class GetStatusSourceText(id: String) :
  MastodonAPIRequest<GetStatusSourceText.Response>(
    method = HttpMethod.GET,
    path = "/statuses/$id/source",
    respClass = Response::class.java
  ) {
  @AllFieldsAreRequired
  class Response : BaseModel() {
    var id: String? = null

    @JvmField
    var text: String? = null

    @JvmField
    var spoilerText: String? = null
  }
}

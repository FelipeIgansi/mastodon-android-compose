package org.joinmastodon.android.api.requests.statuses

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Status

class GetStatusesByIDs(ids: MutableCollection<String>) :
  MastodonAPIRequest<MutableList<Status>>(
    method = HttpMethod.GET,
    path = "/statuses",
    respTypeToken = object : TypeToken<MutableList<Status>>() {}) {
  init {
    ids.forEach { addQueryParameter("id[]", it) }
  }
}

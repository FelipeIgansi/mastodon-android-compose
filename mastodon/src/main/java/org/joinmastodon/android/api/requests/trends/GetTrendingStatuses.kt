package org.joinmastodon.android.api.requests.trends

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Status

class GetTrendingStatuses(offset: Int, limit: Int)
  : MastodonAPIRequest<MutableList<Status?>?>(
  method = HttpMethod.GET,
  path = "/trends/statuses",
  respTypeToken = object : TypeToken<MutableList<Status?>?>() {}) {
  init {
    if (limit > 0) addQueryParameter("limit", "" + limit)
    if (offset > 0) addQueryParameter("offset", "" + offset)
  }
}

package org.joinmastodon.android.api.requests.trends

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Card

class GetTrendingLinks(limit: Int) : MastodonAPIRequest<MutableList<Card?>?>(
  method = HttpMethod.GET,
  path = "/trends/links",
  respTypeToken = object : TypeToken<MutableList<Card?>?>() {}) {
  init {
    addQueryParameter("limit", limit.toString())
  }
}

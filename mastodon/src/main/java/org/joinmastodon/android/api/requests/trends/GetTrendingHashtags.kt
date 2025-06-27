package org.joinmastodon.android.api.requests.trends

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Hashtag

class GetTrendingHashtags(limit: Int) : MastodonAPIRequest<MutableList<Hashtag?>?>(
  method = HttpMethod.GET,
  path = "/trends",
  respTypeToken = object : TypeToken<MutableList<Hashtag?>?>() {}) {
  init {
    addQueryParameter("limit", limit.toString() + "")
  }
}

package org.joinmastodon.android.api.requests.timelines

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Status

class GetHashtagTimeline(
  hashtag: String?,
  maxID: String?,
  minID: String?,
  limit: Int
) : MastodonAPIRequest<MutableList<Status?>?>(
  method = HttpMethod.GET,
  path = "/timelines/tag/$hashtag",
  respTypeToken = object : TypeToken<MutableList<Status?>?>() {}) {
  init {
    if (maxID != null) addQueryParameter("max_id", maxID)
    if (minID != null) addQueryParameter("min_id", minID)
    if (limit > 0) addQueryParameter("limit", "" + limit)
  }
}

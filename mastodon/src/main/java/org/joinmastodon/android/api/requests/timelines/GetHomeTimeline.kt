package org.joinmastodon.android.api.requests.timelines

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Status

class GetHomeTimeline(
  maxID: String?,
  minID: String?,
  limit: Int,
  sinceID: String?
) : MastodonAPIRequest<MutableList<Status>>(
  method = HttpMethod.GET,
  path = "/timelines/home",
  respTypeToken = object : TypeToken<MutableList<Status>>() {}) {
  init {
    if (maxID != null) addQueryParameter("max_id", maxID)
    if (minID != null) addQueryParameter("min_id", minID)
    if (sinceID != null) addQueryParameter("since_id", sinceID)
    if (limit > 0) addQueryParameter("limit", "" + limit)
  }
}

package org.joinmastodon.android.api.requests.timelines

import android.text.TextUtils
import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Status

class GetPublicTimeline(
  local: Boolean,
  remote: Boolean,
  maxID: String?,
  minID: String?,
  limit: Int,
  sinceID: String?
) : MastodonAPIRequest<MutableList<Status?>?>(
  method = HttpMethod.GET,
  path = "/timelines/public",
  respTypeToken = object : TypeToken<MutableList<Status?>?>() {}) {
  init {
    if (local) addQueryParameter("local", "true")
    if (remote) addQueryParameter("remote", "true")
    if (!TextUtils.isEmpty(maxID)) addQueryParameter("max_id", maxID.toString())
    if (!TextUtils.isEmpty(minID)) addQueryParameter("min_id", minID.toString())
    if (!TextUtils.isEmpty(sinceID)) addQueryParameter("since_id", sinceID.toString())
    if (limit > 0) addQueryParameter("limit", limit.toString() + "")
  }
}

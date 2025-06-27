package org.joinmastodon.android.api.requests.tags

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.requests.HeaderPaginationRequest
import org.joinmastodon.android.model.Hashtag
import org.joinmastodon.android.model.HeaderPaginationList

class GetFollowedTags(maxID: String?, limit: Int) : HeaderPaginationRequest<Hashtag?>(
  HttpMethod.GET,
  "/followed_tags",
  object : TypeToken<HeaderPaginationList<Hashtag?>?>() {}) {
  init {
    if (maxID != null) addQueryParameter("max_id", maxID)
    if (limit > 0) addQueryParameter("limit", limit.toString() + "")
  }
}

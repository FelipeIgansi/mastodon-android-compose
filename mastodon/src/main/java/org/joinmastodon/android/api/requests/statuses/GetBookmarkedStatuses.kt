package org.joinmastodon.android.api.requests.statuses

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.requests.HeaderPaginationRequest
import org.joinmastodon.android.model.HeaderPaginationList
import org.joinmastodon.android.model.Status

class GetBookmarkedStatuses(maxID: String?, limit: Int) :
  HeaderPaginationRequest<Status>(
    method = HttpMethod.GET,
    path = "/bookmarks",
    respTypeToken = object : TypeToken<HeaderPaginationList<Status>>() {}
  ) {
  init {
    if (maxID != null) addQueryParameter("max_id", maxID)
    if (limit > 0) addQueryParameter("limit", limit.toString() + "")
  }
}

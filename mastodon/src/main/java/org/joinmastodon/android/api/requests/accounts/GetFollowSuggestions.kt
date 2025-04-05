package org.joinmastodon.android.api.requests.accounts

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.FollowSuggestion

class GetFollowSuggestions(limit: Int) :
  MastodonAPIRequest<MutableList<FollowSuggestion>>(
    HttpMethod.GET,
    "/suggestions",
    object : TypeToken<MutableList<FollowSuggestion>>() {}
  ) {

  init {
    addQueryParameter("limit", "$limit")
  }

  // Mantive o protected para manter o mesma lógica do código original
  protected override fun getPathPrefix() = "/api/v2"
}

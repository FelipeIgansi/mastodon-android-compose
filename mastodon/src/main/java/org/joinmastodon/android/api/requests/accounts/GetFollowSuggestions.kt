package org.joinmastodon.android.api.requests.accounts

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.FollowSuggestion

class GetFollowSuggestions(limit: Int) :
  MastodonAPIRequest<MutableList<FollowSuggestion>>(
    method = HttpMethod.GET,
    path = "/suggestions",
    respTypeToken = object : TypeToken<MutableList<FollowSuggestion>>() {}
  ) {

  init {
    addQueryParameter("limit", "$limit")
  }

  // Mantive o protected para manter o mesma lógica do código original
  override fun getPathPrefix() = "/api/v2"
}

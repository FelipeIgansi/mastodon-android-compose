package org.joinmastodon.android.api.requests.accounts

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Relationship

class GetAccountRelationships(ids: Collection<String>) :
  MastodonAPIRequest<MutableList<Relationship>>(
    method = HttpMethod.GET,
    path = "/accounts/relationships",
    respTypeToken = object : TypeToken<MutableList<Relationship>>() {}) {
  init {
    ids.forEach { id ->
      addQueryParameter("id[]", id)
    }
  }
}

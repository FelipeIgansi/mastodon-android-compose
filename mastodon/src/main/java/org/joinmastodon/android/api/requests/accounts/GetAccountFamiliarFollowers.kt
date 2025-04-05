package org.joinmastodon.android.api.requests.accounts

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.FamiliarFollowers

class GetAccountFamiliarFollowers(ids: Collection<String>) :
/**
 * No código original (Java) é utilizado o List, porem o java não faz diferença de mutavel para imutavel,
 * enquanto o Kotlin sim, portanto coloquei para melhor compatibilidade como mutavel.
 * */
  MastodonAPIRequest<MutableList<FamiliarFollowers>>(
    HttpMethod.GET,
    "/accounts/familiar_followers",
    object : TypeToken<MutableList<FamiliarFollowers>>() {}) {
  init {
    ids.forEach { id ->
      addQueryParameter("id[]", id)
    }
  }
}

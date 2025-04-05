package org.joinmastodon.android.api.requests.accounts

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Hashtag

class GetAccountFeaturedHashtags(id: String) :
/**
 * No código original (Java) é utilizado o List, porem o java não faz diferença de mutavel para imutavel,
 * enquanto o Kotlin sim, portanto coloquei para melhor compatibilidade como mutavel.
 * */
  MastodonAPIRequest<MutableList<Hashtag>>(
    HttpMethod.GET,
    "/accounts/$id/featured_tags",
    object : TypeToken<MutableList<Hashtag>>() {}
  )

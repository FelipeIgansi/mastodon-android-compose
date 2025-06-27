package org.joinmastodon.android.api.requests.instance

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Emoji

class GetCustomEmojis : MastodonAPIRequest<MutableList<Emoji?>?>(
  method = HttpMethod.GET,
  path = "/custom_emojis",
  respTypeToken = object : TypeToken<MutableList<Emoji?>?>() {})

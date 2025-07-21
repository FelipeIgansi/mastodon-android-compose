package org.joinmastodon.android.api.requests.tags

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Hashtag

class GetTag(tag: String) :
  MastodonAPIRequest<Hashtag>(
    method = HttpMethod.GET,
    path = "/tags/$tag",
    respClass = Hashtag::class.java
  )

package org.joinmastodon.android.api.requests.tags

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Hashtag

class SetTagFollowed(tag: String?, followed: Boolean) :
  MastodonAPIRequest<Hashtag>(
    method = HttpMethod.POST,
    path = "/tags/$tag${if (followed) "/follow" else "/unfollow"}",
    respClass = Hashtag::class.java
  ) {
  init {
    setRequestBody(Any())
  }
}

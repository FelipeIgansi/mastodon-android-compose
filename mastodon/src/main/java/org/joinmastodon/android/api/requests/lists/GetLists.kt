package org.joinmastodon.android.api.requests.lists

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.FollowList

class GetLists : MastodonAPIRequest<MutableList<FollowList>>(
  method = HttpMethod.GET,
  path = "/lists",
  respTypeToken = object : TypeToken<MutableList<FollowList>>() {}
)

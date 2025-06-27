package org.joinmastodon.android.api.requests.accounts

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.FollowList

class GetAccountLists(id: String) :
  MastodonAPIRequest<MutableList<FollowList>>(
    method = HttpMethod.GET,
    path = "/accounts/$id/lists",
    respTypeToken = object : TypeToken<MutableList<FollowList>>() {}
)

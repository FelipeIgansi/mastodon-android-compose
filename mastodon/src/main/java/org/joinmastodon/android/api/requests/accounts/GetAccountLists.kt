package org.joinmastodon.android.api.requests.accounts

import com.google.gson.reflect.TypeToken
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.FollowList

class GetAccountLists(id: String) :
  MastodonAPIRequest<MutableList<FollowList>>(
    HttpMethod.GET,
    "/accounts/$id/lists",
    object : TypeToken<MutableList<FollowList>>() {}
  )

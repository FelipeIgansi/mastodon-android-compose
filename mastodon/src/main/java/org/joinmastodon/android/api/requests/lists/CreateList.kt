package org.joinmastodon.android.api.requests.lists

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.FollowList
import org.joinmastodon.android.model.FollowList.RepliesPolicy

class CreateList(
    title: String,
    repliesPolicy: RepliesPolicy,
    exclusive: Boolean
) : MastodonAPIRequest<FollowList>(
    method = HttpMethod.POST,
    path = "/lists",
    respClass = FollowList::class.java
) {

    init {
        setRequestBody(Request(title, repliesPolicy, exclusive))
    }

    private data class Request(
        val title: String,
        val repliesPolicy: RepliesPolicy,
        val exclusive: Boolean
    )
}

package org.joinmastodon.android.api.requests.lists

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.FollowList
import org.joinmastodon.android.model.FollowList.RepliesPolicy

class UpdateList(
    listID: String,
    title: String,
    repliesPolicy: RepliesPolicy,
    exclusive: Boolean
) :
    MastodonAPIRequest<FollowList>(
        method = HttpMethod.PUT,
        path = "/lists/$listID",
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

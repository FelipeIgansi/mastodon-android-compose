package org.joinmastodon.android.api.requests.polls

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.Poll

class SubmitPollVote(pollID: String, choices: List<Int>) :
    MastodonAPIRequest<Poll>(
        method = HttpMethod.POST,
        path = "/polls/$pollID/votes",
        respClass = Poll::class.java
    ) {
    init {
        setRequestBody(Body(choices))
    }

    private data class Body(val choices: List<Int>)
}

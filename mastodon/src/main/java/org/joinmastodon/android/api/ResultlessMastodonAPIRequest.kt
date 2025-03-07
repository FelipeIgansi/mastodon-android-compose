package org.joinmastodon.android.api

abstract class ResultlessMastodonAPIRequest(
    method: HttpMethod,
    path: String
) : MastodonAPIRequest<Unit>(method, path, Unit::class.java)
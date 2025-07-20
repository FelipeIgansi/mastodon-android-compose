package org.joinmastodon.android.api.requests.search

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.SearchResults

class GetSearchResults(
    query: String,
    type: Type?,
    resolve: Boolean,
    maxID: String?,
    offset: Int,
    count: Int
) :
    MastodonAPIRequest<SearchResults>(
        method = HttpMethod.GET,
        path = "/search",
        respClass = SearchResults::class.java
    ) {
    init {
        addQueryParameter("q", query)

        if (type != null) addQueryParameter("type", type.name.lowercase())
        if (resolve) addQueryParameter("resolve", "true")
        if (maxID != null) addQueryParameter("max_id", maxID)
        if (offset > 0) addQueryParameter("offset", offset.toString())
        if (count > 0) addQueryParameter("limit", count.toString())
    }

    fun limit(limit: Int) = apply { addQueryParameter("limit", limit.toString()) }

    override fun getPathPrefix() = "/api/v2"

    enum class Type {
        ACCOUNTS,
        HASHTAGS,
        STATUSES
    }
}

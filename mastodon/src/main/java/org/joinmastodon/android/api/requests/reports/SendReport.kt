package org.joinmastodon.android.api.requests.reports

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.model.ReportReason

class SendReport(
    accountID: String,
    reason: ReportReason,
    statusIDs: List<String>?,
    ruleIDs: List<String>?,
    comment: String?,
    forward: Boolean
) :
    MastodonAPIRequest<Any>(
        method = HttpMethod.POST,
        path = "/reports",
        respClass = Any::class.java
    ) {
    init {
        setRequestBody(
            Body(
                accountId = accountID,
                statusIds = statusIDs ?: emptyList(),
                comment = comment ?: "",
                forward = forward,
                category = reason,
                ruleIds = ruleIDs ?: emptyList()
            )
        )
    }

    private data class Body(
        val accountId: String,
        val statusIds: List<String>,
        val comment: String,
        val forward: Boolean,
        val category: ReportReason,
        val ruleIds: List<String>
    )
}

package org.joinmastodon.android.api

class MastodonDetailedErrorResponse(
    error: String?,
    httpStatus: Int,
    exception: Throwable?
) : MastodonErrorResponse(error, httpStatus, exception) {
    var detailedErrors: Map<String, List<FieldError>>? = emptyMap()

    class FieldError {
        var error: String? = null
        var description: String? = null
    }
}

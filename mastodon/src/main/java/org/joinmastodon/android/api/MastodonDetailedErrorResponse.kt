package org.joinmastodon.android.api

class MastodonDetailedErrorResponse(
    error: String,
    httpStatus: Int,
    exception: Throwable?
) : MastodonErrorResponse(error, httpStatus, exception) {
    var detailedErrors: Map<String, List<FieldError>>? = emptyMap()

    class FieldError {
		@JvmField
		var error: String? = null
		@JvmField
        var description: String? = null
    }
}

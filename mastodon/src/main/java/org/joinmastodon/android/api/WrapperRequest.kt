package org.joinmastodon.android.api

import me.grishka.appkit.api.APIRequest

/**
 * Wraps a different API request to allow a chain of requests to be canceled
 */
class WrapperRequest<T> : APIRequest<T>() {
    @JvmField
		var wrappedRequest: APIRequest<*>? = null

    override fun cancel() {
        if (wrappedRequest != null) wrappedRequest!!.cancel()
    }

    override fun exec(): APIRequest<T>? {
        throw UnsupportedOperationException()
    }
}

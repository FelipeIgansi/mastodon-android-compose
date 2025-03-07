package org.joinmastodon.android.api

import okhttp3.RequestBody
import okio.BufferedSink
import okio.Okio
import okio.Source
import java.io.IOException

internal abstract class CountingRequestBody(
    protected var progressListener: ProgressListener?
) : RequestBody() {
    @JvmField //Used only to communicate with Java code, if it is calling this object.
    protected var length: Long = 0

    @Throws(IOException::class)
    override fun contentLength(): Long {
        return length
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        if (progressListener != null) {
            openSource().use { source ->
                val wrappedSink = Okio.buffer(CountingSink(length, progressListener!!, sink))
                wrappedSink.writeAll(source)
                wrappedSink.flush()
            }
        } else {
            openSource().use { source ->
                sink.writeAll(source)
            }
        }
    }

    @Throws(IOException::class)
    protected abstract fun openSource(): Source
}

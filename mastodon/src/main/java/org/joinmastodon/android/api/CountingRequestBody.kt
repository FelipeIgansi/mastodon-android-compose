package org.joinmastodon.android.api

import okhttp3.RequestBody
import okio.BufferedSink
import okio.Source
import okio.buffer
import java.io.IOException

abstract class CountingRequestBody(
    private var progressListener: ProgressListener?
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
                val wrappedSink = CountingSink(length, progressListener!!, sink).buffer()
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

package org.joinmastodon.android.api

import android.os.SystemClock
import okio.Buffer
import okio.ForwardingSink
import okio.Sink
import org.joinmastodon.android.ui.utils.UiUtils
import java.io.IOException

internal class CountingSink(
    private val length: Long,
    private val progressListener: ProgressListener,
    delegate: Sink
) : ForwardingSink(delegate) {
    private var bytesWritten: Long = 0
    private var lastCallbackTime: Long = 0

    @Throws(IOException::class)
    override fun write(source: Buffer, byteCount: Long) {
        super.write(source, byteCount)
        bytesWritten += byteCount

        if (SystemClock.uptimeMillis() - lastCallbackTime >= 100L ||
            bytesWritten == length
        ) {
            lastCallbackTime = SystemClock.uptimeMillis()
            UiUtils.runOnUiThread { progressListener.onProgress(bytesWritten, length) }
        }
    }
}

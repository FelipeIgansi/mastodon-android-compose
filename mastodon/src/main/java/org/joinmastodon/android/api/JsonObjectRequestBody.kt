package org.joinmastodon.android.api

import com.google.gson.JsonElement
import com.google.gson.JsonIOException
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody
import okio.BufferedSink
import java.io.IOException
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets

class JsonObjectRequestBody(private val obj: Any) : RequestBody() {
    override fun contentType(): MediaType? {
        return "application/json".toMediaType()
    }

    @Throws(IOException::class)
    override fun writeTo(sink: BufferedSink) {
        try {
            val writer = OutputStreamWriter(sink.outputStream(), StandardCharsets.UTF_8)

            if (obj is JsonElement)
                writer.write(obj.toString())
            else
                MastodonAPIController.gson.toJson(obj, writer)

            writer.flush()
        } catch (x: JsonIOException) {
            throw IOException(x)
        }
    }
}

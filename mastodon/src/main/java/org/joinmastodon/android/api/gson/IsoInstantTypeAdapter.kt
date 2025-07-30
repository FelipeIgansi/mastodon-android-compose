package org.joinmastodon.android.api.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAccessor

class IsoInstantTypeAdapter : TypeAdapter<Instant?>() {
  @Throws(IOException::class)
  override fun write(out: JsonWriter, value: Instant?) {
    if (value == null) out.nullValue()
    else out.value(DateTimeFormatter.ISO_INSTANT.format(value))
  }

  @Throws(IOException::class)
  override fun read(reader: JsonReader): Instant? {
    if (reader.peek() == JsonToken.NULL) {
      reader.nextNull()
      return null
    }
    val nextString = try {
      reader.nextString()
    } catch (e: Exception) {
      return null
    }

    return try {
      DateTimeFormatter.ISO_INSTANT.parse(nextString) {
        temporal: TemporalAccessor? -> Instant.from(temporal)
      }
    } catch (x: DateTimeParseException) {
      try {
        return DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(
          nextString
        ) { temporal: TemporalAccessor? -> Instant.from(temporal) }
      } catch (_: DateTimeParseException) {
        null
      }
    }
  }
}

package org.joinmastodon.android.api.gson

import com.google.gson.TypeAdapter
import com.google.gson.stream.JsonReader
import com.google.gson.stream.JsonToken
import com.google.gson.stream.JsonWriter
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeParseException

class IsoLocalDateTypeAdapter : TypeAdapter<LocalDate?>() {
  @Throws(IOException::class)
  override fun write(out: JsonWriter, value: LocalDate?) {
    if (value == null) out.nullValue()
    else out.value(value.toString())
  }

  @Throws(IOException::class)
  override fun read(reader: JsonReader): LocalDate? {
    if (reader.peek() == JsonToken.NULL) {
      reader.nextNull()
      return null
    }
    return try {
      LocalDate.parse(reader.nextString())
    } catch (e: DateTimeParseException) {
      // Log the error or handle it as needed
      null
    }
  }
}

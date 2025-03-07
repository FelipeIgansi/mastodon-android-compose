package org.joinmastodon.android.api.gson

import com.google.gson.JsonArray
import com.google.gson.JsonElement

class JsonArrayBuilder {
  private val arr = JsonArray()

  fun add(element: JsonElement): JsonArrayBuilder = apply { arr.add(element) }

  fun add(element: String): JsonArrayBuilder = apply { arr.add(element) }

  fun add(element: Number): JsonArrayBuilder = apply { arr.add(element) }

  fun add(element: Boolean): JsonArrayBuilder = apply { arr.add(element) }

  fun add(element: JsonObjectBuilder): JsonArrayBuilder = apply { arr.add(element.build()) }

  fun add(element: JsonArrayBuilder): JsonArrayBuilder = apply { arr.add(element.build()) }

  fun build(): JsonArray = arr
}

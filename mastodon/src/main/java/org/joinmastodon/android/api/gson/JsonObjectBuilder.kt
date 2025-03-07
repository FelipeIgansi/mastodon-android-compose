package org.joinmastodon.android.api.gson

import com.google.gson.JsonElement
import com.google.gson.JsonObject

class JsonObjectBuilder {
  private val obj = JsonObject()

  fun add(key: String, el: JsonElement): JsonObjectBuilder = apply { obj.add(key, el) }

  fun add(key: String, el: String): JsonObjectBuilder = apply { obj.addProperty(key, el) }

  fun add(key: String, el: Number): JsonObjectBuilder = apply { obj.addProperty(key, el) }

  fun add(key: String, el: Boolean): JsonObjectBuilder = apply { obj.addProperty(key, el) }

  fun add(key: String, el: JsonObjectBuilder): JsonObjectBuilder = apply { obj.add(key, el.build()) }

  fun add(key: String, el: JsonArrayBuilder): JsonObjectBuilder = apply { obj.add(key, el.build()) }

  fun build(): JsonObject = obj
}

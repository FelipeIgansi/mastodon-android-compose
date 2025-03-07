package org.joinmastodon.android.api

import com.google.gson.annotations.SerializedName
import java.util.EnumSet

object ApiUtils {
    @JvmStatic
    fun <E : Enum<E>> enumSetToStrings(
        e: EnumSet<E>,
        cls: Class<E>
    ): List<String> {
        return e.map { ev ->
            try {
                val annotation = cls.getField(ev.name).getAnnotation(SerializedName::class.java)
                annotation?.value ?: ev.name.lowercase()
            } catch (x: NoSuchFieldException) {
                throw RuntimeException(x)
            }
        }
    }
}

package org.joinmastodon.android.utils

import java.util.LinkedList
import java.util.Objects
import java.util.function.Function

class TypedObjectPool<K, V>(private val producer: Function<K?, V?>) {
  private val pool = HashMap<K?, LinkedList<V?>?>()

  fun obtain(type: K?): V? {
    var tp = pool[type]
    if (tp == null) pool[type] = LinkedList<V?>().also { tp = it }

    var value = tp!!.poll()
    if (value == null) value = producer.apply(type)
    return value
  }

  fun reuse(type: K?, obj: V?) {
    Objects.requireNonNull<V?>(obj)
    Objects.requireNonNull<K?>(type)

    var tp = pool[type]
    if (tp == null) pool[type] = LinkedList<V?>().also { tp = it }
    tp!!.add(obj)
  }
}

package org.joinmastodon.android.utils

class ObjectIdComparator : Comparator<String?> {
  override fun compare(o1: String?, o2: String?): Int {
    val l1 = o1?.length ?: 0
    val l2 = o2?.length ?: 0
    if (l1 != l2) return l1.compareTo(l2)
    if (l1 == 0) return 0
    return o1!!.compareTo(o2!!)
  }

  companion object {
    @JvmField
		val INSTANCE: ObjectIdComparator = ObjectIdComparator()
  }
}

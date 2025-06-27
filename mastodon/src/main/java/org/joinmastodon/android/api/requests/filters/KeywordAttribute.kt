package org.joinmastodon.android.api.requests.filters

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
internal class KeywordAttribute(
  var id: String?,
  @field:SerializedName("_destroy") var delete: Boolean?,
  var keyword: String?,
  var wholeWord: Boolean?
)

package org.joinmastodon.android.api.session

import com.google.gson.annotations.SerializedName

data class AccountActivationInfo(
	@JvmField //Used only to communicate with Java code, if it is calling this object.
  @field:SerializedName(value = "email", alternate = ["a"])
  var email: String,
  @JvmField //Used only to communicate with Java code, if it is calling this object.
  @field:SerializedName(value = "last_email_confirmation_resend", alternate = ["b"])
  var lastEmailConfirmationResend: Long
)

package org.joinmastodon.android.api.requests.accounts

import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.api.RequiredField
import org.joinmastodon.android.model.BaseModel

class CheckInviteLink(path: String) :
  MastodonAPIRequest<CheckInviteLink.Response>(
    HttpMethod.GET, path, Response::class.java
  ) {

  init {
    addHeader("Accept", "application/json")
  }

  protected override fun getPathPrefix() = ""

  class Response (
    @RequiredField
    @JvmField
    var inviteCode: String = ""
  ): BaseModel()
}

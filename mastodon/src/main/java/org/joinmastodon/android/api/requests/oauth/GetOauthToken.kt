package org.joinmastodon.android.api.requests.oauth

import com.google.gson.annotations.SerializedName
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.api.session.AccountSessionManager.Companion.SCOPE
import org.joinmastodon.android.api.session.AccountSessionManager.Companion.REDIRECT_URI
import org.joinmastodon.android.model.Token

class GetOauthToken(
  clientID: String,
  clientSecret: String,
  code: String?,
  grantType: GrantType
) :  MastodonAPIRequest<Token>(
    method = HttpMethod.POST,
    path = "/oauth/token",
    respClass = Token::class.java
  ) {
  init {
    setRequestBody(
      Request(
        clientID,
        clientSecret,
        code,
        grantType
      )
    )
  }

  override fun getPathPrefix() = ""

  private data class Request(
    val clientId: String,
    val clientSecret: String,
    val code: String?,
    val grantType: GrantType,
    val redirectUri: String = REDIRECT_URI,
    val scope: String = SCOPE
  )

  enum class GrantType {
    @SerializedName("authorization_code")
    AUTHORIZATION_CODE,

    @SerializedName("client_credentials")
    CLIENT_CREDENTIALS
  }
}

package org.joinmastodon.android.api.requests.accounts

import android.net.Uri
import okhttp3.MultipartBody
import okhttp3.RequestBody
import org.joinmastodon.android.api.AvatarResizedImageRequestBody
import org.joinmastodon.android.api.MastodonAPIRequest
import org.joinmastodon.android.api.ResizedImageRequestBody
import org.joinmastodon.android.model.Account
import org.joinmastodon.android.model.AccountField
import org.joinmastodon.android.ui.utils.UiUtils
import java.io.File
import java.io.IOException




class UpdateAccountCredentials : MastodonAPIRequest<Account> {
  private val displayName: String
  private val bio: String
  private var avatar: Uri? = null
  private var cover: Uri? = null
  private var avatarFile: File? = null
  private var coverFile: File? = null
  private val fields: List<AccountField>?
  private var discoverable: Boolean? = null
  private var indexable: Boolean? = null
  

  constructor(
    displayName: String,
    bio: String,
    avatar: Uri?,
    cover: Uri?,
    fields: List<AccountField>?
  ) : super(HttpMethod.PATCH, "/accounts/update_credentials", Account::class.java) {
    this.displayName = displayName
    this.bio = bio
    this.avatar = avatar
    this.cover = cover
    this.fields = fields
  }

  constructor(
    displayName: String,
    bio: String,
    avatar: File?,
    cover: File?,
    fields: List<AccountField>?
  ) : super(HttpMethod.PATCH, "/accounts/update_credentials", Account::class.java) {
    this.displayName = displayName
    this.bio = bio
    this.avatarFile = avatar
    this.coverFile = cover
    this.fields = fields
  }

  fun setDiscoverableIndexable(
    discoverable: Boolean,
    indexable: Boolean
  ): UpdateAccountCredentials {
    this.discoverable = discoverable
    this.indexable = indexable
    return this
  }

  @Throws(IOException::class)
  override fun getRequestBody(): RequestBody {
    val builder = MultipartBody.Builder().apply {
      setType(MultipartBody.FORM)
      addFormDataPart("display_$NAME", displayName)
      addFormDataPart("note", bio)


      avatar?.let {
        addFormDataPart(
          AVATAR,
          UiUtils.getFileName(it),
          AvatarResizedImageRequestBody(it, null)
        )
      } ?:
      avatarFile?.let {
        addFormDataPart(
          AVATAR,
          it.name,
          AvatarResizedImageRequestBody(Uri.fromFile(it), null)
        )
      }

      val maxSize = 1500 * 500
      
      cover?.let {
        addFormDataPart(
          HEADER,
          UiUtils.getFileName(it),
          ResizedImageRequestBody(it, maxSize, null)
        )
      } ?:
      coverFile?.let {
        addFormDataPart(
          HEADER,
          it.name, 
          ResizedImageRequestBody(Uri.fromFile(it), maxSize, null)
        )
      }


      fields?.let {
        if (fields.isEmpty()) {
          addFormDataPart("$FIELDS_ATTRIBUTES[0][$NAME]", "")
          addFormDataPart("$FIELDS_ATTRIBUTES[0][$VALUE]", "")
        } else {
          fields.forEachIndexed { index, field ->
            addFormDataPart("$FIELDS_ATTRIBUTES[$index][$NAME]", field.name)
            addFormDataPart("$FIELDS_ATTRIBUTES[$index][$VALUE]", field.value)
          }
        }
      }

      discoverable?.let { addFormDataPart(DISCOVERABLE, it.toString()) }
      indexable?.let { addFormDataPart(INDEXABLE, it.toString()) }
    }


    return builder.build()
  }
}


private const val AVATAR = "avatar"
private const val HEADER = "header"

private const val FIELDS_ATTRIBUTES = "fields_attributes"

private const val NAME = "name"
private const val VALUE = "value"

private const val DISCOVERABLE = "discoverable"
private const val INDEXABLE = "indexable"
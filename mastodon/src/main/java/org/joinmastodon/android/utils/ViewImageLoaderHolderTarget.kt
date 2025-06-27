package org.joinmastodon.android.utils

import android.graphics.drawable.Drawable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import me.grishka.appkit.imageloader.ImageLoaderViewHolder
import me.grishka.appkit.imageloader.ViewImageLoader

class ViewImageLoaderHolderTarget(
  private val holder: ImageLoaderViewHolder,
  private val imageIndex: Int
) : ViewImageLoader.Target {
  override fun setImageDrawable(drawable: Drawable?) {
    if (drawable == null) holder.clearImage(imageIndex)
    else holder.setImage(imageIndex, drawable)
  }

  override fun getView(): View {
    return (holder as RecyclerView.ViewHolder).itemView
  }
}

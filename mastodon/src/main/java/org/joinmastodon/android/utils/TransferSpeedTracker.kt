package org.joinmastodon.android.utils

import android.os.SystemClock
import kotlin.math.roundToLong

class TransferSpeedTracker {

  private var lastKnownPos: Long = 0
  private var lastKnownPosTime: Long = 0
  private var lastSpeed: Double = 0.0
  private var averageSpeed: Double = 0.0
  private var totalBytes: Long = 0

  fun addSample(position: Long) {
    if (lastKnownPosTime == 0L) {
      lastKnownPosTime = SystemClock.uptimeMillis()
      lastKnownPos = position
    } else {
      val time = SystemClock.uptimeMillis()
      lastSpeed = (position - lastKnownPos) / ((time - lastKnownPosTime).toDouble() / 1000.0)
      lastKnownPos = position
      lastKnownPosTime = time
    }
  }

  fun updateAndGetETA(): Long { // must be called at a constant interval
    averageSpeed = if (averageSpeed == 0.0) lastSpeed
    else SMOOTHINGFACTOR * lastSpeed + (1.0 - SMOOTHINGFACTOR) * averageSpeed
    return ((totalBytes - lastKnownPos) / averageSpeed).roundToLong()
  }

  fun setTotalBytes(totalBytes: Long) {
    this.totalBytes = totalBytes
  }

  fun reset() {
    lastKnownPosTime = 0
    lastKnownPos = 0
    averageSpeed = 0.0
    lastSpeed = averageSpeed
    totalBytes = 0
  }

  companion object {
    const val SMOOTHINGFACTOR = 0.05
  }
}

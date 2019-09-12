package com.ke.cameralibrary.util

import android.content.Context
import android.util.DisplayMetrics
import android.view.WindowManager

object ScreenUtils {
  fun getScreenHeight(context: Context): Int {
    val metric = DisplayMetrics()
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    wm.defaultDisplay.getMetrics(metric)
    return metric.heightPixels
  }

  fun getScreenWidth(context: Context): Int {
    val metric = DisplayMetrics()
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    wm.defaultDisplay.getMetrics(metric)
    return metric.widthPixels
  }
}

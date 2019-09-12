package com.ke.cameralibrary.util

import android.util.Log

import com.ke.cameralibrary.BuildConfig.DEBUG

object LogUtil {

  private val DEFAULT_TAG = "LogUtil"

  fun i(tag: String, msg: String) {
    //        if (DEBUG)
    Log.i(tag, msg)
  }

  fun v(tag: String, msg: String) {
    if (DEBUG)
      Log.v(tag, msg)
  }

  fun d(tag: String, msg: String) {
    if (DEBUG)
      Log.d(tag, msg)
  }

  fun e(tag: String, msg: String) {
    if (DEBUG)
      Log.e(tag, msg)
  }

  fun i(msg: String) {
    i(DEFAULT_TAG, msg)
  }

  fun v(msg: String) {
    v(DEFAULT_TAG, msg)
  }

  fun d(msg: String) {
    d(DEFAULT_TAG, msg)
  }

  fun e(msg: String) {
    e(DEFAULT_TAG, msg)
  }
}

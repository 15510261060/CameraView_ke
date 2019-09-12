package com.zxing.cameraapplication

import android.app.Application
import android.content.Context

import com.squareup.leakcanary.LeakCanary
import com.squareup.leakcanary.RefWatcher

class CameraApplication : Application() {

  private val refWatcher: RefWatcher? = null
  override fun onCreate() {
    super.onCreate()
    if (LeakCanary.isInAnalyzerProcess(this)) {
      // This process is dedicated to LeakCanary for heap analysis.
      // You should not init your app in this process.
      return
    }
    LeakCanary.install(this)
    //         Normal app init code...
  }

  companion object {

    fun getRefWatcher(context: Context): RefWatcher? {
      val application = context.applicationContext as CameraApplication
      return application.refWatcher
    }
  }
}

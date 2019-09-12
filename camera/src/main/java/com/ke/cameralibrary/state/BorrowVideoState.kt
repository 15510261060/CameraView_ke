package com.ke.cameralibrary.state

import android.view.Surface
import android.view.SurfaceHolder

import com.ke.cameralibrary.CameraInterface
import com.ke.cameralibrary.JCameraView
import com.ke.cameralibrary.util.LogUtil

class BorrowVideoState(private val machine: CameraMachine) : State {
  private val TAG = "BorrowVideoState"

  override fun start(holder: SurfaceHolder, screenProp: Float) {
    CameraInterface.instance!!.doStartPreview(holder, screenProp)
    machine.state = machine.previewState
  }

  override fun stop() {

  }

  override fun foucs(x: Float, y: Float, callback: CameraInterface.FocusCallback) {

  }

  override fun swtich(holder: SurfaceHolder, screenProp: Float) {

  }

  override fun restart() {

  }

  override fun capture() {

  }

  override fun record(surface: Surface, screenProp: Float) {

  }

  override fun stopRecord(isShort: Boolean, time: Long) {

  }

  override fun cancle(holder: SurfaceHolder, screenProp: Float) {
    machine.view.resetState(JCameraView.TYPE_VIDEO)
    machine.state = machine.previewState
  }

  override fun confirm() {
    machine.view.confirmState(JCameraView.TYPE_VIDEO)
    machine.state = machine.previewState
  }

  override fun zoom(zoom: Float, type: Int) {
    LogUtil.i(TAG, "zoom")
  }

  override fun flash(mode: String) {

  }
}

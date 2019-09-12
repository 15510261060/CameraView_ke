package com.ke.cameralibrary.util

import android.hardware.Camera
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log

object CheckPermission {
  val STATE_RECORDING = -1
  val STATE_NO_PERMISSION = -2
  val STATE_SUCCESS = 1

  /**
   * 用于检测是否具有录音权限
   *
   * @return
   */
  //检测是否可以进入初始化状态
  //6.0以下机型都会返回此状态，故使用时需要判断bulid版本
  //检测是否在录音中
  //检测是否可以获取录音结果
  val recordState: Int
    get() {
      val minBuffer = AudioRecord.getMinBufferSize(
        44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat
          .ENCODING_PCM_16BIT
      )
      var audioRecord: AudioRecord? = AudioRecord(
        MediaRecorder.AudioSource.DEFAULT, 44100, AudioFormat
          .CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, minBuffer * 100
      )
      val point = ShortArray(minBuffer)
      var readSize = 0
      try {

        audioRecord!!.startRecording()
      } catch (e: Exception) {
        if (audioRecord != null) {
          audioRecord.release()
          audioRecord = null
        }
        return STATE_NO_PERMISSION
      }

      if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
        if (audioRecord != null) {
          audioRecord.stop()
          audioRecord.release()
          audioRecord = null
          Log.d("CheckAudioPermission", "录音机被占用")
        }
        return STATE_RECORDING
      } else {

        readSize = audioRecord.read(point, 0, point.size)


        if (readSize <= 0) {
          if (audioRecord != null) {
            audioRecord.stop()
            audioRecord.release()
            audioRecord = null

          }
          Log.d("CheckAudioPermission", "录音的结果为空")
          return STATE_NO_PERMISSION

        } else {
          if (audioRecord != null) {
            audioRecord.stop()
            audioRecord.release()
            audioRecord = null

          }

          return STATE_SUCCESS
        }
      }
    }

  @Synchronized fun isCameraUseable(cameraID: Int): Boolean {
    var canUse = true
    var mCamera: Camera? = null
    try {
      mCamera = Camera.open(cameraID)
      // setParameters 是针对魅族MX5。MX5通过Camera.open()拿到的Camera对象不为null
      val mParameters = mCamera!!.parameters
      mCamera.parameters = mParameters
    } catch (e: Exception) {
      e.printStackTrace()
      canUse = false
    } finally {
      if (mCamera != null) {
        mCamera.release()
      } else {
        canUse = false
      }
      mCamera = null
    }
    return canUse
  }
}
package com.ke.cameralibrary

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.YuvImage
import android.hardware.Camera
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.widget.ImageView

import com.ke.cameralibrary.listener.ErrorListener
import com.ke.cameralibrary.util.AngleUtil
import com.ke.cameralibrary.util.CameraParamUtil
import com.ke.cameralibrary.util.CheckPermission
import com.ke.cameralibrary.util.DeviceUtil
import com.ke.cameralibrary.util.FileUtil
import com.ke.cameralibrary.util.LogUtil
import com.ke.cameralibrary.util.ScreenUtils

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.util.ArrayList

import android.graphics.Bitmap.createBitmap

class CameraInterface private constructor() : Camera.PreviewCallback {

  private var mCamera: Camera? = null
  private var mParams: Camera.Parameters? = null
  private var isPreviewing = false

  private var SELECTED_CAMERA = -1
  private var CAMERA_POST_POSITION = -1
  private var CAMERA_FRONT_POSITION = -1

  private var mHolder: SurfaceHolder? = null
  private var screenProp = -1.0f

  private var isRecorder = false
  private var mediaRecorder: MediaRecorder? = null
  private var videoFileName: String? = null
  private var saveVideoPath: String? = null
  private var videoFileAbsPath: String? = null
  private var videoFirstFrame: Bitmap? = null

  private var errorLisenter: ErrorListener? = null

  private var mSwitchView: ImageView? = null
  //private ImageView mFlashLamp;

  private var preview_width: Int = 0
  private var preview_height: Int = 0

  private var angle = 0
  private var cameraAngle = 90//摄像头角度   默认为90度
  private var rotation = 0
  private var firstFrame_data: ByteArray? = null
  private var nowScaleRate = 0
  private var recordScleRate = 0

  //视频质量
  private var mediaQuality = JCameraView.MEDIA_QUALITY_MIDDLE
  private var sm: SensorManager? = null

  private val sensorEventListener = object : SensorEventListener {
    override fun onSensorChanged(event: SensorEvent) {
      if (Sensor.TYPE_ACCELEROMETER != event.sensor.type) {
        return
      }
      val values = event.values
      angle = AngleUtil.getSensorAngle(values[0], values[1])
      rotationAnimation()
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
  }

  /**
   * 拍照
   */
  private var nowAngle: Int = 0

  internal var handlerTime = 0

  fun setSwitchView(mSwitchView: ImageView?/*, ImageView mFlashLamp*/) {
    this.mSwitchView = mSwitchView
    //this.mFlashLamp = mFlashLamp;
    if (mSwitchView != null) {
      cameraAngle = CameraParamUtil.instance.getCameraDisplayOrientation(
        mSwitchView.context,
        SELECTED_CAMERA
      )
    }
  }

  //切换摄像头icon跟随手机角度进行旋转
  private fun rotationAnimation() {
    if (mSwitchView == null) {
      return
    }
    if (rotation != angle) {
      var start_rotaion = 0
      var end_rotation = 0
      when (rotation) {
        0 -> {
          start_rotaion = 0
          when (angle) {
            90 -> end_rotation = -90
            270 -> end_rotation = 90
          }
        }
        90 -> {
          start_rotaion = -90
          when (angle) {
            0 -> end_rotation = 0
            180 -> end_rotation = -180
          }
        }
        180 -> {
          start_rotaion = 180
          when (angle) {
            90 -> end_rotation = 270
            270 -> end_rotation = 90
          }
        }
        270 -> {
          start_rotaion = 90
          when (angle) {
            0 -> end_rotation = 0
            180 -> end_rotation = 180
          }
        }
      }
      val animC = ObjectAnimator.ofFloat(mSwitchView, "rotation", start_rotaion as Float, end_rotation as Float)
      //ObjectAnimator animF = ObjectAnimator.ofFloat(mFlashLamp, "rotation", start_rotaion, end_rotation);
      val set = AnimatorSet()
      set.playTogether(animC/*, animF*/)
      set.duration = 500
      set.start()
      rotation = angle
    }
  }

  internal fun setSaveVideoPath(saveVideoPath: String) {
    this.saveVideoPath = saveVideoPath
    val file = File(saveVideoPath)
    if (!file.exists()) {
      file.mkdirs()
    }
  }

  fun setZoom(zoom: Float, type: Int) {
    if (mCamera == null) {
      return
    }
    if (mParams == null) {
      mParams = mCamera!!.parameters
    }
    if (!mParams!!.isZoomSupported || !mParams!!.isSmoothZoomSupported) {
      return
    }
    when (type) {
      TYPE_RECORDER -> {
        //如果不是录制视频中，上滑不会缩放
        if (!isRecorder) {
          return
        }
        if (zoom >= 0) {
          //每移动50个像素缩放一个级别
          val scaleRate = (zoom / 40).toInt()
          if (scaleRate <= mParams!!.maxZoom && scaleRate >= nowScaleRate && recordScleRate != scaleRate) {
            mParams!!.zoom = scaleRate
            mCamera!!.parameters = mParams
            recordScleRate = scaleRate
          }
        }
      }
      TYPE_CAPTURE -> {
        if (isRecorder) {
          return
        }
        //每移动50个像素缩放一个级别
        val scaleRate = (zoom / 50).toInt()
        if (scaleRate < mParams!!.maxZoom) {
          nowScaleRate += scaleRate
          if (nowScaleRate < 0) {
            nowScaleRate = 0
          } else if (nowScaleRate > mParams!!.maxZoom) {
            nowScaleRate = mParams!!.maxZoom
          }
          mParams!!.zoom = nowScaleRate
          mCamera!!.parameters = mParams
        }
        LogUtil.i("setZoom = $nowScaleRate")
      }
    }

  }

  internal fun setMediaQuality(quality: Int) {
    this.mediaQuality = quality
  }

  override fun onPreviewFrame(data: ByteArray, camera: Camera) {
    firstFrame_data = data
  }

  fun setFlashMode(flashMode: String) {
    if (mCamera == null)
      return
    val params = mCamera!!.parameters
    params.flashMode = flashMode
    mCamera!!.parameters = params
  }

  interface CameraOpenOverCallback {
    fun cameraHasOpened()
  }

  init {
    findAvailableCameras()
    SELECTED_CAMERA = CAMERA_POST_POSITION
    saveVideoPath = ""
  }

  /**
   * open Camera
   */
  internal fun doOpenCamera(callback: CameraOpenOverCallback) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
      if (!CheckPermission.isCameraUseable(SELECTED_CAMERA) && this.errorLisenter != null) {
        this.errorLisenter!!.onError()
        return
      }
    }
    if (mCamera == null) {
      openCamera(SELECTED_CAMERA)
    }
    callback.cameraHasOpened()
  }

  private fun setFlashModel() {
    mParams = mCamera!!.parameters
    mParams!!.flashMode = Camera.Parameters.FLASH_MODE_TORCH //设置camera参数为Torch模式
    mCamera!!.parameters = mParams
  }

  @Synchronized private fun openCamera(id: Int) {
    try {
      this.mCamera = Camera.open(id)
    } catch (var3: Exception) {
      var3.printStackTrace()
      if (this.errorLisenter != null) {
        this.errorLisenter!!.onError()
      }
    }

    if (Build.VERSION.SDK_INT > 17 && this.mCamera != null) {
      try {
        this.mCamera!!.enableShutterSound(false)
      } catch (e: Exception) {
        e.printStackTrace()
        Log.e("TAG", "enable shutter sound faild")
      }

    }
  }

  @Synchronized fun switchCamera(holder: SurfaceHolder, screenProp: Float) {
    if (SELECTED_CAMERA == CAMERA_POST_POSITION) {
      SELECTED_CAMERA = CAMERA_FRONT_POSITION
    } else {
      SELECTED_CAMERA = CAMERA_POST_POSITION
    }
    doDestroyCamera()
    LogUtil.i("open start")
    openCamera(SELECTED_CAMERA)
    //        mCamera = Camera.open();
    if (Build.VERSION.SDK_INT > 17 && this.mCamera != null) {
      try {
        this.mCamera!!.enableShutterSound(false)
      } catch (e: Exception) {
        e.printStackTrace()
      }

    }
    LogUtil.i("open end")
    doStartPreview(holder, screenProp)
  }

  /**
   * doStartPreview
   */
  fun doStartPreview(holder: SurfaceHolder?, screenProp: Float) {
    if (isPreviewing) {
      LogUtil.i("doStartPreview isPreviewing")
    }
    if (this.screenProp < 0) {
      this.screenProp = screenProp
    }
    if (holder == null) {
      return
    }
    this.mHolder = holder
    if (mCamera != null) {
      try {
        mParams = mCamera!!.parameters
        val previewSize = CameraParamUtil.instance.getPreviewSize(
          mParams!!
            .supportedPreviewSizes, 1000, screenProp
        )
        val pictureSize = CameraParamUtil.instance.getPictureSize(
          mParams!!
            .supportedPictureSizes, 1200, screenProp
        )

        mParams!!.setPreviewSize(previewSize.width, previewSize.height)

        preview_width = previewSize.width
        preview_height = previewSize.height

        mParams!!.setPictureSize(pictureSize.width, pictureSize.height)

        if (CameraParamUtil.instance.isSupportedFocusMode(
            mParams!!.supportedFocusModes,
            Camera.Parameters.FOCUS_MODE_AUTO
          )
        ) {
          mParams!!.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
        }
        if (CameraParamUtil.instance.isSupportedPictureFormats(
            mParams!!.supportedPictureFormats,
            ImageFormat.JPEG
          )
        ) {
          mParams!!.pictureFormat = ImageFormat.JPEG
          mParams!!.jpegQuality = 100
        }
        mCamera!!.parameters = mParams
        mParams = mCamera!!.parameters
        mCamera!!.setPreviewDisplay(holder)  //SurfaceView
        mCamera!!.setDisplayOrientation(cameraAngle)//浏览角度
        mCamera!!.setPreviewCallback(this) //每一帧回调
        mCamera!!.startPreview()//启动浏览
        isPreviewing = true
        Log.i(TAG, "=== Start Preview ===")
      } catch (e: IOException) {
        e.printStackTrace()
      }

    }
  }

  /**
   * 停止预览
   */
  fun doStopPreview() {
    if (null != mCamera) {
      try {
        mCamera!!.setPreviewCallback(null)
        mCamera!!.stopPreview()
        //这句要在stopPreview后执行，不然会卡顿或者花屏
        mCamera!!.setPreviewDisplay(null)
        isPreviewing = false
        Log.i(TAG, "=== Stop Preview ===")
      } catch (e: IOException) {
        e.printStackTrace()
      }

    }
  }

  /**
   * 销毁Camera
   */
  internal fun doDestroyCamera() {
    errorLisenter = null
    if (null != mCamera) {
      try {
        mCamera!!.setPreviewCallback(null)
        mSwitchView = null
        //mFlashLamp = null;
        mCamera!!.stopPreview()
        //这句要在stopPreview后执行，不然会卡顿或者花屏
        mCamera!!.setPreviewDisplay(null)
        mHolder = null
        isPreviewing = false
        mCamera!!.release()
        mCamera = null
        //                destroyCameraInterface();
        Log.i(TAG, "=== Destroy Camera ===")
      } catch (e: IOException) {
        e.printStackTrace()
      }

    } else {
      Log.i(TAG, "=== Camera  Null===")
    }
  }

  fun takePicture(callback: TakePictureCallback?) {
    if (mCamera == null) {
      return
    }
    when (cameraAngle) {
      90 -> nowAngle = Math.abs(angle + cameraAngle) % 360
      270 -> nowAngle = Math.abs(cameraAngle - angle)
    }
    //
    Log.i("TAG", "$angle = $cameraAngle = $nowAngle")
    mCamera!!.takePicture(null, null, Camera.PictureCallback { data, camera ->
      var bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
      val matrix = Matrix()
      if (SELECTED_CAMERA == CAMERA_POST_POSITION) {
        matrix.setRotate(nowAngle.toFloat())
      } else if (SELECTED_CAMERA == CAMERA_FRONT_POSITION) {
        matrix.setRotate((360 - nowAngle).toFloat())
        matrix.postScale(-1f, 1f)
      }

      bitmap = createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
      if (callback != null) {
        if (nowAngle == 90 || nowAngle == 270) {
          callback.captureResult(bitmap, true)
        } else {
          callback.captureResult(bitmap, false)
        }
      }
    })
  }

  //启动录像
  fun startRecord(surface: Surface, screenProp: Float) {
    mCamera!!.setPreviewCallback(null)
    val nowAngle = (angle + 90) % 360
    //获取第一帧图片
    val parameters = mCamera!!.parameters
    val width = parameters.previewSize.width
    val height = parameters.previewSize.height
    val yuv = YuvImage(firstFrame_data, parameters.previewFormat, width, height, null)
    val out = ByteArrayOutputStream()
    yuv.compressToJpeg(Rect(0, 0, width, height), 50, out)
    val bytes = out.toByteArray()
    videoFirstFrame = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    val matrix = Matrix()
    if (SELECTED_CAMERA == CAMERA_POST_POSITION) {
      matrix.setRotate(nowAngle.toFloat())
    } else if (SELECTED_CAMERA == CAMERA_FRONT_POSITION) {
      matrix.setRotate(270f)
    }
    videoFirstFrame = createBitmap(
      videoFirstFrame!!, 0, 0, videoFirstFrame!!.width, videoFirstFrame!!
        .height, matrix, true
    )

    if (isRecorder) {
      return
    }
    if (mCamera == null) {
      openCamera(SELECTED_CAMERA)
    }
    if (mediaRecorder == null) {
      mediaRecorder = MediaRecorder()
    }
    if (mParams == null) {
      mParams = mCamera!!.parameters
    }
    val focusModes = mParams!!.supportedFocusModes
    if (focusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
      mParams!!.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO
    }
    mCamera!!.parameters = mParams
    mCamera!!.unlock()
    mediaRecorder!!.reset()
    mediaRecorder!!.setCamera(mCamera)
    mediaRecorder!!.setVideoSource(MediaRecorder.VideoSource.CAMERA)

    mediaRecorder!!.setAudioSource(MediaRecorder.AudioSource.MIC)

    mediaRecorder!!.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
    mediaRecorder!!.setVideoEncoder(MediaRecorder.VideoEncoder.H264)
    mediaRecorder!!.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)

    val videoSize: Camera.Size
    if (mParams!!.supportedVideoSizes == null) {
      videoSize = CameraParamUtil.instance.getPreviewSize(
        mParams!!.supportedPreviewSizes, 600,
        screenProp
      )
    } else {
      videoSize = CameraParamUtil.instance.getPreviewSize(
        mParams!!.supportedVideoSizes, 600,
        screenProp
      )
    }
    Log.i(TAG, "setVideoSize    width = " + videoSize.width + "height = " + videoSize.height)
    if (videoSize.width == videoSize.height) {
      mediaRecorder!!.setVideoSize(preview_width, preview_height)
    } else {
      mediaRecorder!!.setVideoSize(videoSize.width, videoSize.height)
    }
    //        if (SELECTED_CAMERA == CAMERA_FRONT_POSITION) {
    //            mediaRecorder.setOrientationHint(270);
    //        } else {
    //            mediaRecorder.setOrientationHint(nowAngle);
    ////            mediaRecorder.setOrientationHint(90);
    //        }

    if (SELECTED_CAMERA == CAMERA_FRONT_POSITION) {
      //手机预览倒立的处理
      if (cameraAngle == 270) {
        //横屏
        if (nowAngle == 0) {
          mediaRecorder!!.setOrientationHint(180)
        } else if (nowAngle == 270) {
          mediaRecorder!!.setOrientationHint(270)
        } else {
          mediaRecorder!!.setOrientationHint(90)
        }
      } else {
        if (nowAngle == 90) {
          mediaRecorder!!.setOrientationHint(270)
        } else if (nowAngle == 270) {
          mediaRecorder!!.setOrientationHint(90)
        } else {
          mediaRecorder!!.setOrientationHint(nowAngle)
        }
      }
    } else {
      mediaRecorder!!.setOrientationHint(nowAngle)
    }


    if (DeviceUtil.isHuaWeiRongyao) {
      mediaRecorder!!.setVideoEncodingBitRate(4 * 100000)
    } else {
      mediaRecorder!!.setVideoEncodingBitRate(mediaQuality)
    }
    mediaRecorder!!.setPreviewDisplay(surface)

    videoFileName = "video_" + System.currentTimeMillis() + ".mp4"
    if (saveVideoPath == "") {
      saveVideoPath = Environment.getExternalStorageDirectory().path
    }
    videoFileAbsPath = saveVideoPath + File.separator + videoFileName
    mediaRecorder!!.setOutputFile(videoFileAbsPath)
    try {
      mediaRecorder!!.prepare()
      mediaRecorder!!.start()
      isRecorder = true
    } catch (e: IllegalStateException) {
      e.printStackTrace()
      Log.i("TAG", "startRecord IllegalStateException")
      if (this.errorLisenter != null) {
        this.errorLisenter!!.onError()
      }
    } catch (e: IOException) {
      e.printStackTrace()
      Log.i("TAG", "startRecord IOException")
      if (this.errorLisenter != null) {
        this.errorLisenter!!.onError()
      }
    } catch (e: RuntimeException) {
      Log.i("TAG", "startRecord RuntimeException")
    }

  }

  //停止录像
  fun stopRecord(isShort: Boolean, callback: StopRecordCallback) {
    if (!isRecorder) {
      return
    }
    if (mediaRecorder != null) {
      mediaRecorder!!.setOnErrorListener(null)
      mediaRecorder!!.setOnInfoListener(null)
      mediaRecorder!!.setPreviewDisplay(null)
      try {
        mediaRecorder!!.stop()
      } catch (e: RuntimeException) {
        e.printStackTrace()
        mediaRecorder = null
        mediaRecorder = MediaRecorder()
      } finally {
        if (mediaRecorder != null) {
          mediaRecorder!!.release()
        }
        mediaRecorder = null
        isRecorder = false
      }
      if (isShort) {
        if (FileUtil.deleteFile(videoFileAbsPath)) {
          callback.recordResult(null, null)
        }
        return
      }
      doStopPreview()
      val fileName = saveVideoPath + File.separator + videoFileName
      callback.recordResult(fileName, videoFirstFrame)
    }
  }

  private fun findAvailableCameras() {
    val info = Camera.CameraInfo()
    val cameraNum = Camera.getNumberOfCameras()
    for (i in 0 until cameraNum) {
      Camera.getCameraInfo(i, info)
      when (info.facing) {
        Camera.CameraInfo.CAMERA_FACING_FRONT -> CAMERA_FRONT_POSITION = info.facing
        Camera.CameraInfo.CAMERA_FACING_BACK -> CAMERA_POST_POSITION = info.facing
      }
    }
  }

  fun handleFocus(context: Context, x: Float, y: Float, callback: FocusCallback) {
    if (mCamera == null) {
      return
    }
    val params = mCamera!!.parameters
    val focusRect = calculateTapArea(x, y, 1f, context)
    mCamera!!.cancelAutoFocus()
    if (params.maxNumFocusAreas > 0) {
      val focusAreas = ArrayList<Camera.Area>()
      focusAreas.add(Camera.Area(focusRect, 800))
      params.focusAreas = focusAreas
    } else {
      Log.i(TAG, "focus areas not supported")
      callback.focusSuccess()
      return
    }
    val currentFocusMode = params.focusMode
    try {
      params.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
      mCamera!!.parameters = params
      mCamera!!.autoFocus { success, camera ->
        if (success || handlerTime > 10) {
          val params = camera.parameters
          params.focusMode = currentFocusMode
          camera.parameters = params
          handlerTime = 0
          callback.focusSuccess()
        } else {
          handlerTime++
          handleFocus(context, x, y, callback)
        }
      }
    } catch (e: Exception) {
      Log.e(TAG, "autoFocus failer")
    }

  }

  internal fun setErrorLinsenter(errorLisenter: ErrorListener) {
    this.errorLisenter = errorLisenter
  }

  interface StopRecordCallback {
    fun recordResult(url: String?, firstFrame: Bitmap?)
  }

  interface ErrorCallback {
    fun onError()
  }

  interface TakePictureCallback {
    fun captureResult(bitmap: Bitmap, isVertical: Boolean)
  }

  interface FocusCallback {
    fun focusSuccess()

  }

  internal fun registerSensorManager(context: Context) {
    if (sm == null) {
      sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    sm!!.registerListener(
      sensorEventListener, sm!!.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager
        .SENSOR_DELAY_NORMAL
    )
  }

  internal fun unregisterSensorManager(context: Context) {
    if (sm == null) {
      sm = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
    sm!!.unregisterListener(sensorEventListener)
  }

  internal fun isPreview(res: Boolean) {
    this.isPreviewing = res
  }

  companion object {

    private val TAG = "CameraInterface"

    val TYPE_RECORDER = 0x090
    val TYPE_CAPTURE = 0x091

    val instance: CameraInterface by lazy(mode = LazyThreadSafetyMode.SYNCHRONIZED) {
      CameraInterface() }

    private fun calculateTapArea(x: Float, y: Float, coefficient: Float, context: Context): Rect {
      val focusAreaSize = 300f
      val areaSize = java.lang.Float.valueOf(focusAreaSize * coefficient)!!.toInt()
      val centerX = (x / ScreenUtils.getScreenWidth(context) * 2000 - 1000).toInt()
      val centerY = (y / ScreenUtils.getScreenHeight(context) * 2000 - 1000).toInt()
      val left = clamp(centerX - areaSize / 2, -1000, 1000)
      val top = clamp(centerY - areaSize / 2, -1000, 1000)
      val rectF = RectF(
        left.toFloat(),
        top.toFloat(),
        (left + areaSize).toFloat(),
        (top + areaSize).toFloat()
      )
      return Rect(
        Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(
          rectF
            .bottom
        )
      )
    }

    private fun clamp(x: Int, min: Int, max: Int): Int {
      if (x > max) {
        return max
      }
      return if (x < min) {
        min
      } else x
    }
  }
}

package com.zxing.cameraapplication

import android.Manifest
import android.annotation.TargetApi
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast

import com.ke.cameralibrary.util.DeviceUtil

class MainActivity : AppCompatActivity() {
  private val GET_PERMISSION_REQUEST = 100 //权限申请自定义码
  private lateinit var photo: ImageView
  private lateinit var device: TextView

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    findViewById(R.id.btn).setOnClickListener { getPermissions() }
    photo = findViewById(R.id.image_photo) as ImageView
    device = findViewById(R.id.device) as TextView
    device.text = DeviceUtil.deviceInfo
  }

  /**
   * 获取权限
   */
  private fun getPermissions() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      if (ContextCompat.checkSelfPermission(
          this,
          Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager
          .PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager
          .PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager
          .PERMISSION_GRANTED
      ) {
        //获取intent对象
        startActivityForResult(Intent(this, CameraActivity::class.java), 100)
        overridePendingTransition(R.anim.anim_tab_pay_open, android.R.anim.fade_out)
      } else {
        //不具有获取权限，需要进行权限申请
        ActivityCompat.requestPermissions(
          this@MainActivity,
          arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CAMERA
          ),
          GET_PERMISSION_REQUEST
        )
      }
    } else {
      startActivityForResult(Intent(this, CameraActivity::class.java), 100)
      overridePendingTransition(R.anim.anim_tab_pay_open, android.R.anim.fade_out)
    }
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
    super.onActivityResult(requestCode, resultCode, data)
    if (resultCode == 101) {
      Log.i("TAG", "picture")
      val path = data.getStringExtra("path")
      photo.setImageBitmap(BitmapFactory.decodeFile(path))
    }
    if (resultCode == 102) {
      Log.i("TAG", "video")
      val path = data.getStringExtra("path")
    }
    if (resultCode == 103) {
      Toast.makeText(this, "请检查相机权限~", Toast.LENGTH_SHORT).show()
    }
  }

  @TargetApi(23)
  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    if (requestCode == GET_PERMISSION_REQUEST) {
      var size = 0
      if (grantResults.size >= 1) {
        val writeResult = grantResults[0]
        //读写内存权限
        val writeGranted = writeResult == PackageManager.PERMISSION_GRANTED//读写内存权限
        if (!writeGranted) {
          size++
        }
        //录音权限
        val recordPermissionResult = grantResults[1]
        val recordPermissionGranted = recordPermissionResult == PackageManager.PERMISSION_GRANTED
        if (!recordPermissionGranted) {
          size++
        }
        //相机权限
        val cameraPermissionResult = grantResults[2]
        val cameraPermissionGranted = cameraPermissionResult == PackageManager.PERMISSION_GRANTED
        if (!cameraPermissionGranted) {
          size++
        }
        if (size == 0) {
          startActivityForResult(Intent(this@MainActivity, CameraActivity::class.java), 100)
        } else {
          Toast.makeText(this, "请到设置-权限管理中开启", Toast.LENGTH_SHORT).show()
        }
      }
    }
  }
}

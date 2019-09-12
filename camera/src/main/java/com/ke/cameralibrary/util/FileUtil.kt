package com.ke.cameralibrary.util

import android.graphics.Bitmap
import android.os.Environment

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object FileUtil {
  private val parentPath = Environment.getExternalStorageDirectory()
  private var storagePath = ""
  private var DST_FOLDER_NAME = "JCamera"

  val isExternalStorageWritable: Boolean
    get() {
      val state = Environment.getExternalStorageState()
      return if (Environment.MEDIA_MOUNTED == state) {
        true
      } else false
    }

  private fun initPath(): String {
    if (storagePath == "") {
      storagePath = parentPath.absolutePath + File.separator + DST_FOLDER_NAME
      val f = File(storagePath)
      if (!f.exists()) {
        f.mkdir()
      }
    }
    return storagePath
  }

  fun saveBitmap(dir: String, b: Bitmap): String {
    DST_FOLDER_NAME = dir
    val path = initPath()
    val dataTake = System.currentTimeMillis()
    val jpegName = path + File.separator + "picture_" + dataTake + ".jpg"
    try {
      val fout = FileOutputStream(jpegName)
      val bos = BufferedOutputStream(fout)
      b.compress(Bitmap.CompressFormat.JPEG, 100, bos)
      bos.flush()
      bos.close()
      return jpegName
    } catch (e: IOException) {
      e.printStackTrace()
      return ""
    }

  }

  fun deleteFile(url: String?): Boolean {
    var result = false
    val file = File(url)
    if (file.exists()) {
      result = file.delete()
    }
    return result
  }
}

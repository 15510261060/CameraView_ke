package com.ke.cameralibrary

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

import com.ke.cameralibrary.util.ScreenUtils

class FoucsView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null,
  defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
  private val size: Int
  private var center_x: Int = 0
  private var center_y: Int = 0
  private var length: Int = 0
  private val mPaint: Paint

  init {
    this.size = ScreenUtils.getScreenWidth(context) / 3
    mPaint = Paint()
    mPaint.isAntiAlias = true
    mPaint.isDither = true
    mPaint.color = -0x11e951ea
    mPaint.strokeWidth = 4f
    mPaint.style = Paint.Style.STROKE
  }

  override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
    super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    center_x = (size / 2.0).toInt()
    center_y = (size / 2.0).toInt()
    length = (size / 2.0).toInt() - 2
    setMeasuredDimension(size, size)
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    canvas.drawRect(
      (center_x - length).toFloat(),
      (center_y - length).toFloat(),
      (center_x + length).toFloat(),
      (center_y + length).toFloat(),
      mPaint
    )
    canvas.drawLine(
      2f,
      (height / 2).toFloat(),
      (size / 10).toFloat(),
      (height / 2).toFloat(),
      mPaint
    )
    canvas.drawLine(
      (width - 2).toFloat(),
      (height / 2).toFloat(),
      (width - size / 10).toFloat(),
      (height / 2).toFloat(),
      mPaint
    )
    canvas.drawLine((width / 2).toFloat(), 2f, (width / 2).toFloat(), (size / 10).toFloat(), mPaint)
    canvas.drawLine(
      (width / 2).toFloat(),
      (height - 2).toFloat(),
      (width / 2).toFloat(),
      (height - size / 10).toFloat(),
      mPaint
    )
  }
}

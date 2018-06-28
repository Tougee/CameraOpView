package com.touge.cameraopview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import com.facebook.rebound.SimpleSpringListener
import com.facebook.rebound.Spring
import com.facebook.rebound.SpringConfig
import com.facebook.rebound.SpringSystem
import com.touge.cameraopview.CameraOpView.Mode.EXPAND
import com.touge.cameraopview.CameraOpView.Mode.NONE
import com.touge.cameraopview.CameraOpView.Mode.PROGRESS

class CameraOpView : View, GestureDetector.OnGestureListener {

  private enum class Mode {
    NONE,
    EXPAND,
    PROGRESS
  }

  private var ringColor = Color.WHITE
  private var circleColor = Color.RED
  private var ringStrokeWidth = context.dip(5f)
  private var progressStrokeWidth = context.dip(4.5f)
  private var circleWidth = 0f
  private var maxCircleWidth = 0f
  private var circleInterval = 3f
  private var progressInterval = 1f
  private var progressStartAngle = -90f
  private var curSweepAngle = 0f
  private var progressRect: RectF? = null
  private var expand = 1.2f
  private var sprintEndValue = 1.0
  private var radius = 0f
  private var rawRadius = 0f
  private var midX = 0f
  private var midY = 0f
  private var maxDuration = 0f

  private var mode = NONE

  private val gestureDetector = GestureDetector(context, this)
  private var callback: CameraOpCallback? = null
  private val spring = SpringSystem.create().createSpring().apply {
    springConfig = SpringConfig.fromOrigamiTensionAndFriction(300.0, 4.0)
    endValue = sprintEndValue
  }

  private val ringPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    color = ringColor
    strokeWidth = ringStrokeWidth
  }

  private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.STROKE
    color = circleColor
    strokeWidth = progressStrokeWidth
    strokeCap = Paint.Cap.ROUND
  }

  private val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
    style = Paint.Style.FILL
    color = circleColor
  }

  constructor(context: Context) : this(context, null)
  constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
  constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr) {
    spring.addListener(object : SimpleSpringListener() {
      override fun onSpringUpdate(spring: Spring) {
        if (mode != EXPAND) {
          return
        }
        val value = spring.currentValue
        radius = value.toFloat() * rawRadius
        invalidate()
      }
    })
  }

  override fun onLayout(changed: Boolean, left: Int, top: Int, right: Int, bottom: Int) {
    if (progressRect == null) {
      val size = width / 1.5f - ringStrokeWidth
      val offset = (width - size * expand) / 2
      progressRect = RectF(offset, offset, width - offset, width - offset)
      rawRadius = size / 2
      radius = rawRadius
      maxCircleWidth = radius - ringStrokeWidth
      circleInterval = maxCircleWidth / (60 * .5f)
      midX = width / 2f
      midY = width / 2f
    }
  }

  override fun onDraw(canvas: Canvas) {
    if (mode == NONE) {
      canvas.drawCircle(midX, midY, radius, ringPaint)
    } else if (mode == EXPAND) {
      if (circleWidth <= maxCircleWidth) {
        circleWidth += circleInterval
      }
      canvas.drawCircle(midX, midY, radius, ringPaint)
      canvas.drawCircle(midX, midY, circleWidth, circlePaint)
      invalidate()
    } else {
      canvas.drawCircle(midX, midY, radius * expand, ringPaint)
      canvas.drawArc(progressRect, progressStartAngle, curSweepAngle, false, progressPaint)
      canvas.drawCircle(midX, midY, maxCircleWidth, circlePaint)
      curSweepAngle += progressInterval
      if (curSweepAngle <= 360 + progressInterval) {
        invalidate()
      } else {
        callback?.onProgressStop(maxDuration)
        clean()
      }
    }
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    val handled = gestureDetector.onTouchEvent(event)
    if (handled) {
      return true
    }
    when (event.action) {
      MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
        if (mode == PROGRESS) {
          callback?.onProgressStop(curSweepAngle / 6f)
        }
        clean()
        return true
      }
    }
    return super.onTouchEvent(event)
  }

  override fun onShowPress(e: MotionEvent?) {
  }

  override fun onSingleTapUp(e: MotionEvent?): Boolean {
    spring.endValue = 1.0
    clean()
    callback?.onClick()
    return true
  }

  override fun onDown(e: MotionEvent?): Boolean {
    spring.endValue = expand.toDouble()
    mode = EXPAND
    return true
  }

  override fun onFling(e1: MotionEvent?, e2: MotionEvent?, velocityX: Float, velocityY: Float): Boolean {
    return false
  }

  override fun onScroll(e1: MotionEvent?, e2: MotionEvent?, distanceX: Float, distanceY: Float): Boolean {
    return false
  }

  override fun onLongPress(e: MotionEvent?) {
    mode = PROGRESS
    spring.endValue = sprintEndValue
    radius = rawRadius
    invalidate()
    callback?.onProgressStart()
  }

  private fun clean() {
    mode = NONE
    curSweepAngle = 0f
    circleWidth = -10f
    radius = rawRadius
    invalidate()
  }

  fun setMaxDuration(duration: Float) {
    maxDuration = duration
    progressInterval = 360f / (60 * duration)
  }

  fun setCameraOpCallback(callback: CameraOpCallback) {
    this.callback = callback
  }

  private fun Context.dip(value: Float): Float = (value * resources.displayMetrics.density)

  interface CameraOpCallback {
    fun onClick()
    fun onProgressStart()
    fun onProgressStop(duration: Float)
  }
}
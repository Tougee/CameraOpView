package com.touge.cameraopview

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.support.v7.app.AppCompatActivity
import android.widget.Toast
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        op.setCameraOpCallback(object : CameraOpView.CameraOpCallback {
            override fun onClick() {
                toast("onClick")
            }

            override fun onProgressStart() {
                this@MainActivity.vibrate(longArrayOf(0, 30))
                toast("onProgressStart")
            }

            override fun onProgressStop() {
                toast("onProgressStop")
            }

        })
    }

    private fun Context.toast(message: CharSequence): Toast = Toast
            .makeText(this, message, Toast.LENGTH_SHORT)
            .apply { show() }

    @Suppress("DEPRECATION")
    fun Context.vibrate(pattern: LongArray) {
        if (Build.VERSION.SDK_INT >= 26) {
            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(VibrationEffect.createWaveform(pattern, -1))
        } else {
            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(pattern, -1)
        }
    }
}

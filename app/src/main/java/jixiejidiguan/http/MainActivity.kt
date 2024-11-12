package jixiejidiguan.http

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity() {

    private lateinit var jumpButton: Button
    private lateinit var countDownTimer: CountDownTimer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 设置沉浸式模式
        window.decorView.systemUiVisibility = (
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR // 如果需要白色文字
                )
        window.statusBarColor = Color.TRANSPARENT

        jumpButton = findViewById(R.id.button)

        // 设置倒计时
        startCountDown()

        // 设置按钮点击事件
        jumpButton.setOnClickListener {
            navigateToNextActivity()
        }
    }


    private fun startCountDown() {
        countDownTimer = object : CountDownTimer(3000, 1000) {
            @SuppressLint("SetTextI18n")
            override fun onTick(millisUntilFinished: Long) {
                jumpButton.text = "${millisUntilFinished / 1000} 跳过"
            }

            override fun onFinish() {
                navigateToNextActivity()
            }
        }.start()
    }

    private fun navigateToNextActivity() {
        countDownTimer.cancel()
        val intent = Intent(this, MainActivity2::class.java)
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        countDownTimer.cancel()
    }
}
   
package com.example.lockapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.Toast

class LockActivity : Activity() {
    private val CORRECT_CODE = "0110"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_lock)

        window.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.setOnKeyListener { _, keyCode, _ ->
            true
        }

        val pinInput = findViewById<EditText>(R.id.pinInput)
        val unlockBtn = findViewById<Button>(R.id.unlockBtn)

        unlockBtn.setOnClickListener {
            val entered = pinInput.text.toString()
            if (entered == CORRECT_CODE) {
                stopLockService()
                finish()
            } else {
                pinInput.text.clear()
                Toast.makeText(this, "Неверный код", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun stopLockService() {
        stopService(Intent(this, LockService::class.java))
    }

    override fun onBackPressed() { /* заглушка */ }
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = true
}

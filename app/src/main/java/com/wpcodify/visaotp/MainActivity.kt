package com.wpcodify.visaotp

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import org.json.JSONObject
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var pair: EditText
    private lateinit var statusPill: TextView
    private lateinit var deliver: TextView
    private lateinit var logView: TextView
    private lateinit var toggleBtn: Button

    private val needPerms = arrayOf(Manifest.permission.RECEIVE_SMS, Manifest.permission.READ_SMS)
    private val handler = Handler(Looper.getMainLooper())
    private var polling = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        pair = findViewById(R.id.pair)
        statusPill = findViewById(R.id.statusPill)
        deliver = findViewById(R.id.deliver)
        logView = findViewById(R.id.log)
        toggleBtn = findViewById(R.id.toggle)

        pair.setText(prefs.pair)
        handleDeepLink(intent) // opened from a scanned QR?

        toggleBtn.setOnClickListener { if (!prefs.enabled) connect() else disconnect() }

        findViewById<Button>(R.id.test).setOnClickListener {
            prefs.pair = pair.text.toString().trim().uppercase()
            val sample = "Your bKash OTP for PAYMENT of Tk.1,500.00 to IVAC (DGePay) is 123456."
            val hit = OtpParser.parse(sample, "bKash")!!
            thread {
                val (ok, note) = Uploader.submit(prefs, hit.otp, hit.source, sample, System.currentTimeMillis(), -1)
                prefs.log("TEST ${if (ok) "✓" else "✗"} $note")
                runOnUiThread { render() }
            }
        }

        findViewById<Button>(R.id.clearLog).setOnClickListener {
            getSharedPreferences("visa_otp", MODE_PRIVATE).edit().putString("log", "").apply()
            render()
        }

        askNotifications()
        ensurePerms()
        render()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    // visaotp://pair?id=RLJ3HS  — from scanning the extension's QR
    private fun handleDeepLink(intent: Intent?) {
        val data = intent?.data ?: return
        if (data.scheme == "visaotp") {
            val id = data.getQueryParameter("id")?.trim()?.uppercase().orEmpty()
            if (id.length in 4..12) {
                pair.setText(id)
                prefs.pair = id
                Toast.makeText(this, "Pair ID বসানো হয়েছে: $id", Toast.LENGTH_SHORT).show()
                connect()
            }
        }
    }

    private fun connect() {
        val code = pair.text.toString().trim().uppercase()
        if (code.length < 4) {
            Toast.makeText(this, "আগে ব্রাউজারের Pair ID বসান (বা QR স্ক্যান করুন)", Toast.LENGTH_SHORT).show()
            return
        }
        prefs.pair = code
        ensurePerms()
        if (!hasPerms()) return
        prefs.enabled = true
        OtpService.start(this)
        prefs.log("▶ চালু হলো — Pair $code")
        render()
    }

    private fun disconnect() {
        prefs.enabled = false
        OtpService.stop(this)
        prefs.log("⏹ বন্ধ করা হলো")
        render()
    }

    override fun onResume() {
        super.onResume()
        render()
        startPolling()
    }

    override fun onPause() {
        super.onPause()
        polling = false
        handler.removeCallbacksAndMessages(null)
    }

    // While the screen is open, show whether the browser has the latest OTP.
    private fun startPolling() {
        if (polling) return
        polling = true
        val tick = object : Runnable {
            override fun run() {
                if (!polling) return
                if (prefs.enabled && prefs.pair.isNotBlank()) {
                    thread {
                        val raw = Uploader.status(prefs)
                        runOnUiThread { showDelivery(raw) }
                    }
                } else {
                    showDelivery(null)
                }
                if (polling) handler.postDelayed(this, 4000)
            }
        }
        handler.post(tick)
    }

    private fun showDelivery(raw: String?) {
        if (raw == null) { deliver.text = ""; deliver.visibility = TextView.GONE; return }
        try {
            val j = JSONObject(raw)
            val last = j.optInt("lastSeq", 0)
            if (last == 0) { deliver.text = "ব্রাউজার প্রস্তুত, OTP-র অপেক্ষায়"; }
            else if (j.optBoolean("delivered", false)) deliver.text = "✓ ব্রাউজারে পৌঁছেছে"
            else deliver.text = "⏳ ব্রাউজারে পাঠানো হচ্ছে…"
            deliver.visibility = TextView.VISIBLE
        } catch (e: Exception) { deliver.visibility = TextView.GONE }
    }

    private fun render() {
        val on = prefs.enabled
        statusPill.text = if (on) "● যুক্ত (OTP পাঠানো হচ্ছে)" else "○ বন্ধ"
        statusPill.setBackgroundResource(if (on) R.drawable.pill_on else R.drawable.pill_off)
        toggleBtn.text = if (on) "বন্ধ করুন" else "কানেক্ট করুন"
        toggleBtn.setBackgroundResource(if (on) R.drawable.btn_stop else R.drawable.btn_go)
        logView.text = prefs.logText.ifBlank { "এখনো কোনো OTP পাঠানো হয়নি।" }
    }

    private fun hasPerms() = needPerms.all {
        ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun ensurePerms() {
        val missing = needPerms.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (missing.isNotEmpty()) ActivityCompat.requestPermissions(this, missing.toTypedArray(), 1)
    }

    private fun askNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 2)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        render()
    }
}

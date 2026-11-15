package com.wpcodify.visaotp

import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/** Posts a parsed OTP to the built-in server. Runs on a background thread. */
object Uploader {

    fun submit(prefs: Prefs, otp: String, source: String, raw: String, smsTs: Long, sim: Int): Pair<Boolean, String> {
        val pair = prefs.pair
        if (pair.isBlank()) return false to "Pair ID দিন"

        return try {
            val conn = (URL(Config.SERVER.trimEnd('/') + "/submit.php").openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                connectTimeout = 8000
                readTimeout = 12000
                doOutput = true
                setRequestProperty("Content-Type", "application/x-www-form-urlencoded")
            }
            val form = buildString {
                append("key=").append(enc(Config.APP_KEY))
                append("&pair=").append(enc(pair))
                append("&otp=").append(enc(otp))
                append("&source=").append(enc(source))
                append("&smsTs=").append(smsTs)
                append("&sim=").append(sim)
                append("&raw=").append(enc(raw.take(300)))
            }
            conn.outputStream.use { os: OutputStream -> os.write(form.toByteArray()) }
            val code = conn.responseCode
            val resp = (if (code in 200..299) conn.inputStream else conn.errorStream)
                ?.bufferedReader()?.readText() ?: ""
            conn.disconnect()
            if (code in 200..299) true to "পাঠানো হয়েছে ($source $otp)"
            else false to "HTTP $code: ${resp.take(80)}"
        } catch (e: Exception) {
            false to ("ত্রুটি: " + (e.message ?: "unknown"))
        }
    }

    /** Asks the server whether the browser has received the latest OTP. */
    fun status(prefs: Prefs): String? {
        val pair = prefs.pair
        if (pair.isBlank()) return null
        return try {
            val conn = (URL(Config.SERVER.trimEnd('/') + "/status.php?pair=" + enc(pair)).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"; connectTimeout = 6000; readTimeout = 8000
            }
            val code = conn.responseCode
            val body = (if (code in 200..299) conn.inputStream else conn.errorStream)?.bufferedReader()?.readText() ?: ""
            conn.disconnect()
            if (code in 200..299) body else null
        } catch (e: Exception) { null }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")
}

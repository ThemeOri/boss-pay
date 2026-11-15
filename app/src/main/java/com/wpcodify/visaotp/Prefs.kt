package com.wpcodify.visaotp

import android.content.Context

/** Only the Pair ID and on/off are user-set; server and key are built in. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("visa_otp", Context.MODE_PRIVATE)

    var pair: String
        get() = sp.getString("pair", "")!!
        set(v) = sp.edit().putString("pair", v.trim().uppercase()).apply()

    var enabled: Boolean
        get() = sp.getBoolean("enabled", false)
        set(v) = sp.edit().putBoolean("enabled", v).apply()

    fun log(line: String) {
        val old = sp.getString("log", "") ?: ""
        sp.edit().putString("log", (line + "\n" + old).take(4000)).apply()
    }

    val logText: String get() = sp.getString("log", "") ?: ""
}

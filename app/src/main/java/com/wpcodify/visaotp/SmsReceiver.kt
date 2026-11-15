package com.wpcodify.visaotp

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.concurrent.thread

/**
 * Fires on every incoming SMS, from BOTH SIM cards, and forwards bKash/Rocket
 * OTPs to the server. Android delivers SMS_RECEIVED for every SIM to the same
 * receiver, so dual-SIM phones are covered; we also read the SIM slot for the log.
 */
class SmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        val prefs = Prefs(context)
        if (!prefs.enabled) return

        // Which SIM/subscription delivered this SMS (for the log; -1 if unknown)
        val sim = intent.getIntExtra("subscription", intent.getIntExtra("subscriptionId", -1))

        // A long SMS arrives in parts; join them per sender and keep the newest time
        val bySender = HashMap<String, StringBuilder>()
        val tsBySender = HashMap<String, Long>()
        for (msg in Telephony.Sms.Intents.getMessagesFromIntent(intent)) {
            val from = msg.originatingAddress ?: "?"
            bySender.getOrPut(from) { StringBuilder() }.append(msg.messageBody ?: "")
            val t = try { msg.timestampMillis } catch (e: Throwable) { System.currentTimeMillis() }
            tsBySender[from] = maxOf(tsBySender[from] ?: 0L, t)
        }

        for ((sender, sb) in bySender) {
            val body = sb.toString()
            val hit = OtpParser.parse(body, sender) ?: continue
            val smsTs = tsBySender[sender] ?: System.currentTimeMillis()
            if (System.currentTimeMillis() - smsTs > 150_000) {
                prefs.log(now() + " · পুরনো SMS বাদ")
                continue
            }
            thread {
                val (ok, note) = Uploader.submit(prefs, hit.otp, hit.source, body, smsTs, sim)
                val simTag = if (sim >= 0) " [SIM $sim]" else ""
                prefs.log("${now()} ${if (ok) "✓" else "✗"} $note$simTag")
            }
        }
    }

    private fun now() = SimpleDateFormat("HH:mm:ss", Locale.US).format(Date())
}

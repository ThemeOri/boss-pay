package com.wpcodify.visaotp

/**
 * Built-in settings. These are compiled into the app, NOT shown in the UI and
 * NOT shareable — anyone you give the app to just enters their Pair ID.
 *
 * APP_KEY must match the APP_KEY in the server's config.php.
 * SERVER is the OTP relay server.
 */
object Config {
    const val APP_KEY = "CHANGE-THIS-TO-A-LONG-RANDOM-STRING"
    const val SERVER = "https://sms2.wpcodify.com"
}

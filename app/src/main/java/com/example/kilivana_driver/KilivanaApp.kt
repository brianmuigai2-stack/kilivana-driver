package com.example.kilivana_driver

import android.app.Application
import coil.Coil
import coil.ImageLoader
import com.example.kilivana_driver.data.network.ApiClient
import com.example.kilivana_driver.data.network.SessionPreferences
import com.example.kilivana_driver.notifications.NotificationHelper

class KilivanaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        ApiClient.install(SessionPreferences(this))
        NotificationHelper.ensureChannels(this)
        // Share the same OkHttpClient so Coil sends the ngrok-skip-browser-warning
        // header and the auth token when loading profile images.
        Coil.setImageLoader(
            ImageLoader.Builder(this)
                .okHttpClient { ApiClient.okHttpClient }
                .respectCacheHeaders(false)
                .build()
        )
    }
}

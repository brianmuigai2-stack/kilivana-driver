package com.example.kilivana_driver

import android.app.Application
import com.example.kilivana_driver.data.network.ApiClient
import com.example.kilivana_driver.data.network.SessionPreferences
import com.example.kilivana_driver.notifications.NotificationHelper

class KilivanaApp : Application() {

    override fun onCreate() {
        super.onCreate()
        // Secure token storage needs a Context, and the network client needs
        // it before any request, so wire it up first thing.
        ApiClient.install(SessionPreferences(this))
        NotificationHelper.ensureChannels(this)
    }
}

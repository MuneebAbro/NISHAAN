package com.maximus.nishaan

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.maximus.nishaan.core.di.AppContainer
import com.maximus.nishaan.core.util.Constants

/**
 * Application entry point. Initializes the manual DI container
 * and notification channels.
 */
class NishaanApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = getSystemService(NotificationManager::class.java)

        val critical = NotificationChannel(
            Constants.CHANNEL_CRITICAL,
            "Critical Alerts",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "High and critical severity crisis alerts"
        }

        val informational = NotificationChannel(
            Constants.CHANNEL_INFORMATIONAL,
            "Informational Alerts",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Low and medium severity crisis alerts"
        }

        val missingPersons = NotificationChannel(
            Constants.CHANNEL_MISSING_PERSONS,
            "Missing Person Matches",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Notifications when a missing person report has a match"
        }

        manager.createNotificationChannels(listOf(critical, informational, missingPersons))
    }
}

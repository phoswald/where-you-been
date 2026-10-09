package com.github.phoswald.whereyoubeen.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import com.github.phoswald.whereyoubeen.R
import com.github.phoswald.whereyoubeen.WhereYouBeenApp
import com.github.phoswald.whereyoubeen.ui.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private const val CHANNEL_ID = "tracking"
private const val NOTIFICATION_ID = 1

/**
 * Keeps the process alive and in the "foreground" while tracking, so GPS updates and uploads
 * continue with the screen locked or another app in front. Runs [LocationTracker.run] until tracking
 * is disabled or the app is swiped away from the recent apps.
 */
class LocationTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var tracking: Job? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        if (tracking == null) {
            val tracker = (application as WhereYouBeenApp).container.locationTracker
            tracking = scope.launch {
                tracker.run()
                stopSelf()
            }
        }
        // Not restarted by the system: a location service cannot be started from the background
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channel = NotificationChannel(
            CHANNEL_ID, getString(R.string.tracking_channel), NotificationManager.IMPORTANCE_LOW
        )
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        val openApp = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentTitle(getString(R.string.tracking_notification))
            .setContentIntent(openApp)
            .setOngoing(true)
            .build()
    }

    companion object {
        /** Must be called while the app is visible, with location permission granted. */
        fun start(context: Context) {
            context.startForegroundService(Intent(context, LocationTrackingService::class.java))
        }
    }
}

package com.oddlyspaced.covidnotifier

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat

class CovidNotificationService: Service() {

    private val tag = "NOTIFICATION_SERVICE"
    val actionStartForegroundService = "ACTION_START_FOREGROUND_SERVICE"
    val actionStopForegroundService = "ACTION_STOP_FOREGROUND_SERVICE"
    private val actionRefresh = "ACTION_REFRESH"
    private val notificationChannelId = "10001"

    override fun onBind(intent: Intent?): IBinder? {
        throw UnsupportedOperationException("Not yet implemented")
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "Service started!")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent != null) {
            when(intent.action) {
                actionStartForegroundService -> {
                    startForegroundService()
                    Toast.makeText(applicationContext, "Service Started", Toast.LENGTH_LONG).show()
                }
                actionStopForegroundService -> {
                    stopForegroundService()
                    Toast.makeText(applicationContext, "Service Stopped", Toast.LENGTH_LONG).show()
                }
                actionRefresh -> {
                    Toast.makeText(applicationContext, "Refreshing...", Toast.LENGTH_LONG).show()
                    refresh()
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    private fun createNotificationChannel() {
        val name = getString(R.string.app_name)
        val descriptionText = getString(R.string.app_name)
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(notificationChannelId, name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(totalCases: Int, todayCases: Int, totalDeaths: Int, todayDeaths: Int, recovered: Int, active: Int) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, 0)

        val message = "Deaths: $totalDeaths (+$todayDeaths)\nRecovered: $recovered\nActive: $active"
        val builder = NotificationCompat.Builder(this, notificationChannelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("India: $totalCases (+$todayCases)")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_MIN)
            // Set the intent that will fire when the user taps the notification
            .setContentIntent(pendingIntent)
            .setAutoCancel(false)
            .setWhen(System.currentTimeMillis())
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))

        val refreshIntent = Intent(this, CovidNotificationService::class.java)
        refreshIntent.action = actionRefresh
        val pendinRefresgIntent = PendingIntent.getService(this, 0, refreshIntent, 0)
        val playAction = NotificationCompat.Action(android.R.drawable.ic_media_play, "Refresh", pendinRefresgIntent)
        builder.addAction(playAction)

        val notification = builder.build()

        startForeground(1, notification)

        Handler().postDelayed(Runnable {
            refresh()
        }, 60 * 60 * 100)
    }

    private lateinit var fetchData: FetchData

    private fun startForegroundService() {
        Log.d(tag, "Starting service...")
        createNotificationChannel()
        fetchData = FetchData()
        fetchData.fetchData()
        checkFetch()
    }

    private fun checkFetch() {
        Handler().postDelayed(Runnable {
            if (fetchData.fetched) {
                createNotification(fetchData.totalCases, fetchData.todayCases, fetchData.totalDeaths, fetchData.todayDeaths, fetchData.recovered, fetchData.active)
            }
            else {
                checkFetch()
            }
        }, 10)
    }

    private fun stopForegroundService() {
        Log.d(tag, "Stopping service...")
        stopForeground(true)
        stopSelf()
    }

    private fun refresh() {
        stopForegroundService()
        val intent = Intent(this, CovidNotificationService::class.java)
        intent.action = CovidNotificationService().actionStartForegroundService
        startService(intent)
    }
}
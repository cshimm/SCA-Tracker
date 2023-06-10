package com.example.scatracker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.scatracker.worker.makeStatusNotification

@Suppress("DEPRECATION")
class RouteTrackingService : Service() {
    //Get reference to the notification builder and service handler
    private lateinit var notificationBuilder: NotificationCompat.Builder
    private lateinit var serviceHandler: Handler

    override fun onCreate() {
        super.onCreate()
        //populate notificationBuilder from the return value of startForegroundService
        notificationBuilder = startForegroundService()
        //define and start the thread for concurrency
        val handlerThread = HandlerThread("RouteTracking").apply { start() }
        //populate serviceHandler with a Handler object using the handlerThread
        serviceHandler = Handler(handlerThread.looper)
    }
    private fun startForegroundService(): NotificationCompat.Builder {
        val pendingIntent = getPendingIntent()
        val channelId = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel()
        } else {
            ""
        }
        //create notificationBuilder with the pending intent and notification channel id
        val notificationBuilder = getNotificationBuilder(pendingIntent, channelId)
        //start the foreground service with the notificationBuilder
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        return notificationBuilder
    }

    //Set up the notification channel with an id and name
    @RequiresApi(Build.VERSION_CODES.O)
    private fun createNotificationChannel(): String {
        val channelId = "routeTracking"
        val channelName = "Route Tracking"
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT)
        val service = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        service.createNotificationChannel(channel)
        return channelId
    }
    //add agent id to mutable list of completed agents
    private fun notifyCompletion(agentId: String) {
        Handler(Looper.getMainLooper()).post {
            mutableTrackingCompletion.value = agentId
        }
    }
    //called from launchTrackingService in MainActivity
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)
        //grab the agent id from the intent string extra, throw error if null
        val agentId = intent?.getStringExtra(EXTRA_SECRET_CAT_AGENT_ID)
            ?: throw IllegalStateException("Agent ID must be provided")
        //start a runnable service by tracking the agent to destination, setting an observer for completion then stop the command
        serviceHandler.post {
            trackToDestination(notificationBuilder)
            notifyCompletion(agentId)
            stopForeground(true)
            stopSelf()
        }
        //return the value from the parent call
        return returnValue
    }
    //tracks cat agent to destination
    private fun trackToDestination(notificationBuilder: NotificationCompat.Builder) {
        for (i in 10 downTo 0) {
            Thread.sleep(1000L)
            notificationBuilder.setContentText("$i seconds to destination")
            startForeground(NOTIFICATION_ID, notificationBuilder.build())

            makeStatusNotification("$i seconds to destination", this)
        }
    }

    //not using binding so set onBind to null
    override fun onBind(intent: Intent): IBinder? = null
    //Get the pending intent from the main activity
    private fun getPendingIntent() =
        PendingIntent.getActivity(
            this,
            0,
            Intent(
                this,
                MainActivity::class.java
            ),
            PendingIntent.FLAG_IMMUTABLE
        )

    //Configure the notifications with title, content, icon.
    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId: String) =
        NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Agent approaching destination")
            .setContentText("Agent dispatched")
            .setContentIntent(pendingIntent)
            .setTicker("Agent dispatched, tracking movement")

    //companion object with id and livedata dealing with task completion for easy referencing
    companion object {
        const val NOTIFICATION_ID = 0xCA7
        const val EXTRA_SECRET_CAT_AGENT_ID = "scaId"
        private val mutableTrackingCompletion = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableTrackingCompletion
    }
}
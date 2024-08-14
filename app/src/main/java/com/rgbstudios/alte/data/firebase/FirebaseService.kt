package com.rgbstudios.alte.data.firebase

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.rgbstudios.alte.R
import com.rgbstudios.alte.ui.activities.MainActivity
import com.rgbstudios.alte.utils.SharedPreferencesManager
import kotlin.random.Random

private const val CHANNEL_ID ="alte_channel"

class FirebaseService: FirebaseMessagingService() {

    companion object {
        var sharedPref: SharedPreferencesManager? = null

        var token: String?
            get() {
                return sharedPref?.getString("token", "")
            }
            set(value) {
                sharedPref?.putString("token", value ?: "")
            }
    }

    override fun onNewToken(newToken: String) {
        super.onNewToken(newToken)

        token = newToken
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val intent =Intent(this, MainActivity::class.java)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = Random.nextInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createNotificationChannel(notificationManager)
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(message.data["title"])
            .setContentText(message.data["message"])
            .setSmallIcon(R.drawable.logo)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(notificationId,  notification)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun  createNotificationChannel(notificationManager: NotificationManager) {
        val channelName = "alteChannel"
        val channel = NotificationChannel(CHANNEL_ID, channelName, IMPORTANCE_HIGH).apply {
            description = "Stay Connected"
            enableLights(true)
            lightColor = Color.YELLOW
        }
        notificationManager.createNotificationChannel(channel)
    }
}
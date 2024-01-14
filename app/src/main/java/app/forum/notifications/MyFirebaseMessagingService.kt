package app.forum.notifications

import android.annotation.TargetApi
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import app.forum.MyCommentsActivity
import app.forum.R
import app.forum.database.FirestoreRepository
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

// This class handles the reception of push notifications
class MyFirebaseMessagingService : FirebaseMessagingService() {

    companion object {
        val TAG = "MyFirebaseMsgService"
    }

    private lateinit var firebaseAnalytics: FirebaseAnalytics
    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create channels to receive push notifications
            val attributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            createNotificationChannel(
                R.string.channel_id,
                R.string.channel_name,
                R.string.channel_description,
                soundUri, attributes
            )
        }
        firebaseAnalytics = Firebase.analytics
    }

    @TargetApi(26)
    private fun createNotificationChannel(
        idId: Int,
        nameId: Int,
        descriptionId: Int,
        sound: Uri?,
        audioAttributes: AudioAttributes?
    ){
        val id = getString(idId)
        val name = getString(nameId)
        val descriptionText = getString(descriptionId)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(id, name, importance)
        channel.description = descriptionText
        channel.setSound(sound, audioAttributes)

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Called if the FCM registration token is updated. This may occur if the security of
     * the previous token had been compromised. Note that this is called when the
     * FCM registration token is initially generated so this is where you would retrieve the token.
     */
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        // Send FCM registration token to server
        sendRegistrationToServer(token)
    }

    private fun sendRegistrationToServer(token: String?) {
        if(token != null) {
            val firebaseRepository = FirestoreRepository()
            firebaseRepository.addToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "From: ${remoteMessage.from}")

        // Check if message contains a data payload.
        remoteMessage.data.isNotEmpty().let {
            Log.d(TAG, "Message data payload: " + remoteMessage.data)
        }

        // Check if message contains a notification payload.
        remoteMessage.notification?.let {
            Log.d(TAG, "Message Notification Body: ${it.body}")
            showNotification(it)
        }
    }

    private fun showNotification(not: RemoteMessage.Notification) {
        val intent = Intent(this, MyCommentsActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0 /* Request code */, intent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) { PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT } else { PendingIntent.FLAG_ONE_SHOT }
        )

        val notificationBuilder = NotificationCompat.Builder(this, getString(R.string.channel_id))
            .setSmallIcon(R.drawable.ic_baseline_reply_24)
            .setContentTitle(not.title)
            .setContentText(not.body)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setDefaults(Notification.DEFAULT_ALL)

        var notification = notificationBuilder.build()

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(not.title.hashCode(), notification)
    }
}
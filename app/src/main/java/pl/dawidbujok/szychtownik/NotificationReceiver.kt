package pl.dawidbujok.szychtownik

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val IS_SPECIAL_DAY_REMINDER = "is_special_day_reminder"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val noteContent = intent.getStringExtra("note_content") ?: "Brak treści"
        val noteId = intent.getIntExtra("note_id", 0)
        val isSpecialDay = intent.getBooleanExtra(IS_SPECIAL_DAY_REMINDER, false)

        val title = if (isSpecialDay) "Przypomnienie: $noteContent" else "Przypomnienie o notatce"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val mainActivityIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context, 
            noteId, 
            mainActivityIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )

        val notification = NotificationCompat.Builder(context, "note_channel")
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(noteContent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(noteId, notification)
    }
}
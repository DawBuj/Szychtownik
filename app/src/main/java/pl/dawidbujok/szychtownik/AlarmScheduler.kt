package pl.dawidbujok.szychtownik

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.time.LocalDate
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(AlarmManager::class.java)

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun schedule(note: Note) {
        if (!canScheduleExactAlarms()) { return }

        note.reminderDateTime?.let {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("note_content", note.content)
                putExtra("note_id", note.id)
                putExtra(NotificationReceiver.IS_SPECIAL_DAY_REMINDER, false)
            }

            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                it.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
                PendingIntent.getBroadcast(
                    context,
                    note.id,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            )
        }
    }

    fun scheduleSpecialDay(instance: SpecialDayInstance, dayType: CustomDayType, date: LocalDate) {
        if (!canScheduleExactAlarms()) { return }

        instance.reminderDateTime?.let {
            val intent = Intent(context, NotificationReceiver::class.java).apply {
                putExtra("note_content", dayType.description)
                val uniqueId = date.hashCode() + dayType.id.hashCode()
                putExtra("note_id", uniqueId)
                putExtra(NotificationReceiver.IS_SPECIAL_DAY_REMINDER, true)
            }

            val uniqueRequestCode = date.hashCode() + dayType.id.hashCode()
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                it.atZone(ZoneId.systemDefault()).toEpochSecond() * 1000,
                PendingIntent.getBroadcast(
                    context,
                    uniqueRequestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )
            )
        }
    }

    fun cancel(noteId: Int) {
        val intent = Intent(context, NotificationReceiver::class.java)
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                noteId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        )
    }

    fun cancelSpecialDay(date: LocalDate, typeId: String) {
        val intent = Intent(context, NotificationReceiver::class.java)
        val uniqueRequestCode = date.hashCode() + typeId.hashCode()
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                uniqueRequestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
        )
    }
}

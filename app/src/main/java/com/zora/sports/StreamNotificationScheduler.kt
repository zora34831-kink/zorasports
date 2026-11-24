
package com.zora.sports

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object StreamNotificationScheduler {

    private const val PREFS = "zora_stream_notifications"
    private const val KEY_ENABLED = "notifications_enabled"
    private const val KEY_IDS = "scheduled_ids"

    private const val CHANNEL_ID = "zora_live_schedule"
    private const val CHANNEL_NAME = "Jadwal Live ZORASPORTS"

    fun isEnabled(context: Context): Boolean {
        return context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).getBoolean(KEY_ENABLED, false)
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        ).edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()

        if (!enabled) {
            cancelAll(context)
        }
    }

    fun sync(
        context: Context,
        streams: List<StreamLink>
    ) {
        if (!isEnabled(context)) {
            cancelAll(context)
            return
        }

        val now = System.currentTimeMillis()

        val upcoming = streams.filter {
            it.published &&
                it.id.isNotBlank() &&
                it.scheduledStartAt > now
        }

        val prefs = context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

        val previousIds = prefs.getStringSet(
            KEY_IDS,
            emptySet()
        )?.toSet() ?: emptySet()

        val currentIds = upcoming.map { it.id }.toSet()

        // Batalkan pengingat yang sudah dihapus atau tidak dipublikasikan.
        (previousIds - currentIds).forEach { id ->
            cancel(context, id)
            prefs.edit().remove("time_$id").apply()
        }

        upcoming.forEach { stream ->
            val oldTime = prefs.getLong(
                "time_${stream.id}",
                0L
            )

            // Jadwal tidak berubah, tidak perlu membuat alarm baru.
            if (oldTime == stream.scheduledStartAt) {
                return@forEach
            }

            cancel(context, stream.id)

            schedule(
                context = context,
                stream = stream
            )

            prefs.edit()
                .putLong(
                    "time_${stream.id}",
                    stream.scheduledStartAt
                )
                .apply()
        }

        prefs.edit()
            .putStringSet(KEY_IDS, currentIds)
            .apply()
    }

    private fun schedule(
        context: Context,
        stream: StreamLink
    ) {
        val alarmManager = context.getSystemService(
            Context.ALARM_SERVICE
        ) as AlarmManager

        val pendingIntent = createPendingIntent(
            context,
            stream
        )

        try {
            if (
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                alarmManager.canScheduleExactAlarms()
            ) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    stream.scheduledStartAt,
                    pendingIntent
                )
            } else {
                // Fallback jika akses alarm tepat belum diberikan.
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    stream.scheduledStartAt,
                    pendingIntent
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                stream.scheduledStartAt,
                pendingIntent
            )
        }
    }

    private fun createPendingIntent(
        context: Context,
        stream: StreamLink
    ): PendingIntent {
        val intent = Intent(
            context,
            StreamReminderReceiver::class.java
        ).apply {
            action = "com.zora.sports.STREAM_REMINDER"
            data = Uri.parse(
                "zorasports://reminder/${stream.id}"
            )

            putExtra("stream_id", stream.id)
            putExtra("match_name", stream.matchName)
            putExtra("scheduled_time", stream.scheduledStartAt)
        }

        return PendingIntent.getBroadcast(
            context,
            stream.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun cancel(
        context: Context,
        id: String
    ) {
        val intent = Intent(
            context,
            StreamReminderReceiver::class.java
        ).apply {
            action = "com.zora.sports.STREAM_REMINDER"
            data = Uri.parse("zorasports://reminder/$id")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            id.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or
                PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            val alarmManager = context.getSystemService(
                Context.ALARM_SERVICE
            ) as AlarmManager

            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun cancelAll(context: Context) {
        val prefs = context.getSharedPreferences(
            PREFS,
            Context.MODE_PRIVATE
        )

        val ids = prefs.getStringSet(
            KEY_IDS,
            emptySet()
        )?.toSet() ?: emptySet()

        ids.forEach { id ->
            cancel(context, id)
        }

        val editor = prefs.edit()
            .remove(KEY_IDS)

        ids.forEach { id ->
            editor.remove("time_$id")
        }

        editor.apply()
    }

    fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description =
                    "Pengingat jadwal pertandingan ZORASPORTS"
            }

            val manager = context.getSystemService(
                NotificationManager::class.java
            )

            manager.createNotificationChannel(channel)
        }
    }

    internal fun showReminder(
        context: Context,
        matchName: String
    ) {
        if (!isEnabled(context)) return

        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        createChannel(context)

        val openApp = Intent(
            context,
            MainActivity::class.java
        )

        val contentIntent = PendingIntent.getActivity(
            context,
            9001,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or
                PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(
            context,
            CHANNEL_ID
        )
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle("Jadwal live sekarang!")
            .setContentText(
                "$matchName — cek halaman Notifikasi ZORASPORTS."
            )
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        "$matchName sudah memasuki jadwal mulai. " +
                            "Cek halaman Notifikasi ZORASPORTS."
                    )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat
            .from(context)
            .notify(matchName.hashCode(), notification)
    }
}

class StreamReminderReceiver : BroadcastReceiver() {

    override fun onReceive(
        context: Context,
        intent: Intent
    ) {
        val matchName = intent.getStringExtra("match_name")
            ?: return

        val streamId = intent.getStringExtra("stream_id")
            ?: return

        val scheduledTime = intent.getLongExtra(
            "scheduled_time",
            0L
        )

        val prefs = context.getSharedPreferences(
            "zora_stream_notifications",
            Context.MODE_PRIVATE
        )

        // Pastikan alarm lama tidak mengirim notifikasi.
        val savedTime = prefs.getLong(
            "time_$streamId",
            0L
        )

        if (
            savedTime != scheduledTime ||
            !StreamNotificationScheduler.isEnabled(context)
        ) {
            return
        }

        StreamNotificationScheduler.showReminder(
            context,
            matchName
        )

        // Hapus jadwal yang sudah selesai dari daftar lokal.
        val ids = prefs.getStringSet(
            "scheduled_ids",
            emptySet()
        )?.toMutableSet() ?: mutableSetOf()

        ids.remove(streamId)

        prefs.edit()
            .putStringSet("scheduled_ids", ids)
            .remove("time_$streamId")
            .apply()
    }
}

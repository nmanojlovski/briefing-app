package com.nmanojlovski.briefing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

private const val WORK_NAME = "briefing-check"
private const val CHANNEL_ID = "daily-briefing"
private const val FIRST_HOUR = 9  // first check at 9:00
private const val LAST_HOUR = 13  // last check at 13:00

// WorkManager runs this in the background, even when the app is closed.
// Each run checks once, then books the next run itself.
class BriefingCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val today = LocalDate.now().toString() // e.g. "2026-11-26", the same format as index.json
        val todays = try {
            briefingApi.getIndex().find { it.date == today }
        } catch (e: Exception) {
            null // no internet or a server problem: treat it like "not published yet"
        }
        if (todays != null) showBriefingNotification(applicationContext, todays)

        // APPEND_OR_REPLACE: queue the next check to start after this one finishes.
        scheduleCheck(applicationContext, foundToday = todays != null, ExistingWorkPolicy.APPEND_OR_REPLACE)
        return Result.success()
    }
}

// Called every time the app opens. KEEP: if a check is already booked, leave it alone;
// if none is (first install, or something went wrong), book one.
fun scheduleDailyCheck(context: Context) {
    scheduleCheck(context, foundToday = false, ExistingWorkPolicy.KEEP)
}

private fun scheduleCheck(context: Context, foundToday: Boolean, policy: ExistingWorkPolicy) {
    val now = LocalDateTime.now()
    val request = OneTimeWorkRequestBuilder<BriefingCheckWorker>()
        .setInitialDelay(Duration.between(now, nextCheckTime(now, foundToday)))
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .build()
    WorkManager.getInstance(context).enqueueUniqueWork(WORK_NAME, policy, request)
}

// Checks run at 9:00, 10:00, 11:00, 12:00 and 13:00, then again tomorrow at 9:00.
// Once today's briefing has been found, skip straight to tomorrow.
fun nextCheckTime(now: LocalDateTime, foundToday: Boolean): LocalDateTime {
    val firstToday = now.toLocalDate().atTime(FIRST_HOUR, 0)
    val lastToday = now.toLocalDate().atTime(LAST_HOUR, 0)
    val nextFullHour = now.truncatedTo(ChronoUnit.HOURS).plusHours(1)
    return when {
        foundToday -> firstToday.plusDays(1)
        now < firstToday -> firstToday              // before 9:00: first check at 9:00
        nextFullHour <= lastToday -> nextFullHour   // between 9:00 and 13:00: next full hour
        else -> firstToday.plusDays(1)              // after 13:00: tomorrow at 9:00
    }
}

fun showBriefingNotification(context: Context, entry: BriefingEntry) {
    val manager = context.getSystemService(NotificationManager::class.java)
    // Every notification belongs to a "channel"; you can switch channels off in the app's settings.
    manager.createNotificationChannel(
        NotificationChannel(CHANNEL_ID, "Daily briefing", NotificationManager.IMPORTANCE_DEFAULT)
    )

    // Tapping restarts the app on its first screen, which shows the newest (= today's) briefing.
    val intent = Intent(context, MainActivity::class.java)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    val onTap = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(R.drawable.ic_notification)
        .setContentTitle("Your morning briefing is ready")
        .setContentText(entry.title)
        .setContentIntent(onTap)
        .setAutoCancel(true) // remove the notification once it's tapped
        .build()

    // If you denied the notification permission, Android quietly drops this.
    manager.notify(1, notification)
}

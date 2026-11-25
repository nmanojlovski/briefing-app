package com.nmanojlovski.briefing

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime

private const val WORK_NAME = "daily-briefing-check"
private const val CHANNEL_ID = "daily-briefing"

// WorkManager runs this in the background around 9:15, even when the app is closed.
class BriefingCheckWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val today = LocalDate.now().toString() // e.g. "2026-11-26", the same format as index.json
        val todays = try {
            briefingApi.getIndex().find { it.date == today }
        } catch (e: Exception) {
            null // no internet or a server problem: treat it like "not published yet"
        }
        if (todays != null) {
            showBriefingNotification(applicationContext, todays)
            return Result.success()
        }
        // Not there yet: WorkManager tries again after 15, 30, 45 and 60 minutes, then gives up for today.
        return if (runAttemptCount < 4) Result.retry() else Result.success()
    }
}

// Schedules the check for the next 9:15, then every day after that.
// Called each time the app opens: WorkManager counts each new day from when the last run
// finished, so without this the time would slowly drift later.
fun scheduleDailyCheck(context: Context) {
    val now = LocalDateTime.now()
    var next = now.toLocalDate().atTime(9, 15)
    if (!next.isAfter(now)) next = next.plusDays(1) // already past 9:15 today: start tomorrow

    val request = PeriodicWorkRequestBuilder<BriefingCheckWorker>(Duration.ofDays(1))
        .setInitialDelay(Duration.between(now, next))
        .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
        .setBackoffCriteria(BackoffPolicy.LINEAR, Duration.ofMinutes(15))
        .build()

    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
        WORK_NAME, ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE, request
    )
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

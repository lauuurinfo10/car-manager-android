package com.example.carmanager.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.carmanager.MainActivity
import com.example.carmanager.R
import com.example.carmanager.data.ApiClient
import com.example.carmanager.domain.Urgency
import com.example.carmanager.domain.daysLeftLabel
import com.example.carmanager.domain.upcomingMaintenance
import com.example.carmanager.ui.display
import com.example.carmanager.ui.km
import java.util.concurrent.TimeUnit

/**
 * Rulează o dată pe zi: verifică actele care expiră și intervențiile scadente și trimite notificări.
 * Ca să nu primești aceeași notificare în fiecare zi, ținem minte ce praguri am anunțat deja
 * (30, 7, 3, 1, 0 zile, expirat) pentru fiecare act.
 */
class ReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val api = ApiClient.api
        val prefs = applicationContext.getSharedPreferences("reminders", Context.MODE_PRIVATE)
        val sent = prefs.getStringSet(KEY_SENT, emptySet())!!.toMutableSet()

        return try {
            val cars = api.listCars().associateBy { it.id }

            for (doc in api.expiringDocuments(30)) {
                val bucket = DOC_THRESHOLDS.firstOrNull { doc.daysLeft <= it } ?: continue
                val key = "doc:${doc.id}:${doc.expiryDate}:$bucket"
                if (key in sent) continue
                val plate = cars[doc.carId]?.licensePlate.orEmpty()
                notify(key.hashCode(), "${doc.title} – $plate", daysLeftLabel(doc.daysLeft.toLong()).replaceFirstChar { it.uppercase() })
                sent += key
            }

            for (car in cars.values) {
                val due = upcomingMaintenance(api.listMaintenance(car.id), car.currentMileage)
                    .filter { it.urgency != Urgency.OK }
                for (u in due) {
                    val key = "maint:${car.id}:${u.rule.name}:${u.last.id}:${u.urgency}"
                    if (key in sent) continue
                    val text = when (u.urgency) {
                        Urgency.OVERDUE -> "Scadentă depășită"
                        else -> "Urmează curând"
                    } + listOfNotNull(u.dueKm?.km(), u.dueDate?.display()).joinToString(" / ", prefix = " (", postfix = ")")
                    notify(key.hashCode(), "${u.rule.name} – ${car.licensePlate}", text)
                    sent += key
                }
            }

            prefs.edit { putStringSet(KEY_SENT, sent) }
            Result.success()
        } catch (_: Throwable) {
            Result.retry()
        }
    }

    private fun notify(id: Int, title: String, text: String) {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val intent = PendingIntent.getActivity(
            ctx, 0,
            Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_car)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        NotificationManagerCompat.from(ctx).notify(id, notification)
    }

    companion object {
        const val CHANNEL_ID = "reminders"
        private const val KEY_SENT = "sent"
        private const val WORK_NAME = "daily-reminders"
        private val DOC_THRESHOLDS = listOf(-1, 0, 1, 3, 7, 30)

        fun createChannel(context: Context) {
            val channel = NotificationChannel(CHANNEL_ID, "Remindere acte și service", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Alerte când expiră RCA, ITP, rovinieta sau urmează o revizie" }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        /** Doar pentru testare: rulează verificarea imediat, o singură dată. */
        fun runNow(context: Context) {
            WorkManager.getInstance(context).enqueue(OneTimeWorkRequestBuilder<ReminderWorker>().build())
        }

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(1, TimeUnit.DAYS)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}

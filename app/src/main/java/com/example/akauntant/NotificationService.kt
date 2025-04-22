package com.example.akauntant

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.Calendar

/**
 * Service class to handle notifications for budget alerts and daily reminders
 * Created by Student for the Akauntant project
 */
class NotificationService {
    
    companion object {
        private const val CHANNEL_ID_BUDGET = "akauntant_budget_channel"
        private const val CHANNEL_ID_REMINDERS = "akauntant_reminders_channel"
        private const val BUDGET_NOTIFICATION_ID = 101
        private const val REMINDER_NOTIFICATION_ID = 102
        private const val HALF_HOURLY_REMINDER_REQUEST_CODE = 1001
        
        /**
         * Creates notification channels for Android O and above
         */
        fun createNotificationChannels(context: Context) {
            // Only needed for Android 8.0 (API 26) and above
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Create Budget Alerts channel
                val budgetChannel = NotificationChannel(
                    CHANNEL_ID_BUDGET,
                    "Budget Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alerts when your spending exceeds the budget limit"
                }
                
                // Create Daily Reminders channel
                val remindersChannel = NotificationChannel(
                    CHANNEL_ID_REMINDERS,
                    "Transaction Reminders",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Reminders to add your transactions"
                }
                
                // Register the channels with the system
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(budgetChannel)
                notificationManager.createNotificationChannel(remindersChannel)
            }
        }
        
        /**
         * Shows a budget alert notification when user exceeds budget limits
         */
        fun showBudgetAlert(context: Context, percentUsed: Int, currencySymbol: String, budgetAmount: Double, spentAmount: Double) {
            // Check if budget alerts are enabled
            val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
            val notificationsEnabled = prefs.getBoolean(SettingsActivity.NOTIFICATIONS_KEY, false)
            val budgetAlertsEnabled = prefs.getBoolean(SettingsActivity.BUDGET_ALERT_KEY, false)
            
            // Don't show if notifications or budget alerts are disabled
            if (!notificationsEnabled || !budgetAlertsEnabled) return
            
            // Create notification content
            val title = "Budget Alert!"
            val message = when {
                percentUsed >= 100 -> "You've exceeded your monthly budget! " +
                        "Spent: $currencySymbol${String.format("%.2f", spentAmount)} " +
                        "of $currencySymbol${String.format("%.2f", budgetAmount)}"
                percentUsed >= 90 -> "You're close to exceeding your monthly budget! " +
                        "Spent: $currencySymbol${String.format("%.2f", spentAmount)} " +
                        "of $currencySymbol${String.format("%.2f", budgetAmount)}"
                percentUsed >= 75 -> "You've spent $percentUsed% of your monthly budget! " +
                        "Spent: $currencySymbol${String.format("%.2f", spentAmount)} " +
                        "of $currencySymbol${String.format("%.2f", budgetAmount)}"
                else -> return // Don't show alert for less than 75%
            }
            
            // Create an intent that will open the app when notification is tapped
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )
            
            // Build the notification
            val builder = NotificationCompat.Builder(context, CHANNEL_ID_BUDGET)
                .setSmallIcon(android.R.drawable.ic_dialog_alert) // Use default Android icon for simplicity
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
            
            // Show the notification
            with(NotificationManagerCompat.from(context)) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notify(BUDGET_NOTIFICATION_ID, builder.build())
                }
            }
        }
        
        /**
         * Schedule periodic reminders to add transactions
         * @param context The application context
         * @param enabled Whether reminders should be enabled or disabled
         */
        fun scheduleDailyReminder(context: Context, enabled: Boolean): Boolean {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, HalfHourReminderReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                HALF_HOURLY_REMINDER_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Cancel any existing alarms
            alarmManager.cancel(pendingIntent)
            
            // If not enabled, return after cancelling
            if (!enabled) {
                return true
            }
            
            // Create the first reminder to start immediately
            val calendar = Calendar.getInstance()
            
            // Schedule repeating alarm every half hour
            // For newer Android versions, we need to manually reschedule in the receiver
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setRepeating(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    30 * 60 * 1000, // 30 minutes in milliseconds
                    pendingIntent
                )
            }
            return true
        }
    }
    
    /**
     * Receiver for half-hourly reminders
     */
    class HalfHourReminderReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            // Check if reminders are still enabled
            val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
            val notificationsEnabled = prefs.getBoolean(SettingsActivity.NOTIFICATIONS_KEY, false)
            val dailyRemindersEnabled = prefs.getBoolean(SettingsActivity.DAILY_REMINDERS_KEY, false)
            
            // Only show notification if both settings are enabled
            if (notificationsEnabled && dailyRemindersEnabled) {
                showReminderNotification(context)
            }
            
            // Reschedule for next half hour (needed for newer Android versions)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                val nextIntent = Intent(context, HalfHourReminderReceiver::class.java)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    HALF_HOURLY_REMINDER_REQUEST_CODE,
                    nextIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                // Set next reminder for 30 minutes from now
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.MINUTE, 30)
                
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
        }
        
        private fun showReminderNotification(context: Context) {
            // Create an intent that will open the add transaction screen when notification is tapped
            val intent = Intent(context, AddTransactionActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            val pendingIntent = PendingIntent.getActivity(
                context, 0, intent, PendingIntent.FLAG_IMMUTABLE
            )
            
            // Build the notification
            val builder = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
                .setSmallIcon(android.R.drawable.stat_sys_warning) // Use default Android icon
                .setContentTitle("Transaction Reminder")
                .setContentText("Don't forget to record your recent income and expenses!")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
            
            // Show the notification
            with(NotificationManagerCompat.from(context)) {
                if (ActivityCompat.checkSelfPermission(
                        context,
                        android.Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notify(REMINDER_NOTIFICATION_ID, builder.build())
                }
            }
        }
    }
    
    /**
     * Receiver that runs on device boot to reschedule notifications
     */
    class BootCompletedReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
                val prefs = context.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE)
                val notificationsEnabled = prefs.getBoolean(SettingsActivity.NOTIFICATIONS_KEY, false)
                val dailyRemindersEnabled = prefs.getBoolean(SettingsActivity.DAILY_REMINDERS_KEY, false)
                
                // Re-schedule reminders if they were enabled
                if (notificationsEnabled && dailyRemindersEnabled) {
                    scheduleDailyReminder(context, true)
                }
            }
        }
    }
}
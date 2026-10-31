package com.example.carmanager

import android.app.Application
import com.example.carmanager.notifications.ReminderWorker

class CarManagerApp : Application() {
    override fun onCreate() {
        super.onCreate()
        ReminderWorker.createChannel(this)
        ReminderWorker.schedule(this)
    }
}

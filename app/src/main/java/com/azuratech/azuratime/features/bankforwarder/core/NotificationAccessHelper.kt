package com.azuratech.azuratime.features.bankforwarder.core

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import com.azuratech.azuratime.features.bankforwarder.service.BankForwarderNotificationListener

object NotificationAccessHelper {

    fun isNotificationServiceEnabled(context: Context): Boolean {
        val componentName = ComponentName(context, BankForwarderNotificationListener::class.java)
        val flat = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners",
        ) ?: return false
        return flat.contains(componentName.flattenToString())
    }
}

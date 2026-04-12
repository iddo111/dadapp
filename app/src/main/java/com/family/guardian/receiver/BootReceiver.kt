package com.family.guardian.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.family.guardian.service.GuardianService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        GuardianService.start(context)
    }
}

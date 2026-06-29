package com.gamehub.host.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

class PowerHelper(private val context: Context) {
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val batteryManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
    } else null

    private val _powerState = MutableLiveData<PowerState>()
    val powerState: LiveData<PowerState> = _powerState

    private val powerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updatePowerState()
        }
    }

    init {
        updatePowerState()
    }

    fun register() {
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                try {
                    addAction(PowerManager.ACTION_DEVICE_IDLE_MODE_CHANGED)
                } catch (_: Exception) {
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    addAction(PowerManager.ACTION_DEVICE_LIGHT_IDLE_MODE_CHANGED)
                } catch (_: Exception) {
                }
            }
        }
        context.registerReceiver(powerReceiver, filter)
    }

    fun unregister() {
        try {
            context.unregisterReceiver(powerReceiver)
        } catch (_: Exception) {
        }
    }

    private fun updatePowerState() {
        val batteryLevel = getBatteryLevel()
        val isCharging = isCharging()
        val isDozeMode = isDeviceIdleMode()
        val isLightIdleMode = isDeviceLightIdleMode()

        _powerState.postValue(
            PowerState(
                batteryLevel = batteryLevel,
                isCharging = isCharging,
                isDozeMode = isDozeMode,
                isLightIdleMode = isLightIdleMode
            )
        )
    }

    private fun getBatteryLevel(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && batteryManager != null) {
            try {
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
            } catch (_: Exception) {
                100
            }
        } else {
            100
        }
    }

    private fun isCharging(): Boolean {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        return status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL
    }

    private fun isDeviceIdleMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                powerManager.javaClass.getMethod("isDeviceIdleMode").invoke(powerManager) as Boolean
            } catch (_: Exception) {
                false
            }
        } else {
            false
        }
    }

    private fun isDeviceLightIdleMode(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                powerManager.javaClass.getMethod("isDeviceLightIdleMode").invoke(powerManager) as Boolean
            } catch (_: Exception) {
                false
            }
        } else {
            false
        }
    }

    data class PowerState(
        val batteryLevel: Int,
        val isCharging: Boolean,
        val isDozeMode: Boolean,
        val isLightIdleMode: Boolean
    )
}

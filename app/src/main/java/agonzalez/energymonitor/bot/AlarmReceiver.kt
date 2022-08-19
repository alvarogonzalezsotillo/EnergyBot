package agonzalez.energymonitor.bot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class AlarmReceiver : BroadcastReceiver() {
    val TAG = "AlarmReceiver"
    init{
        Log.d(TAG, "Receiver  creado")
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "onReceive")
        val extras = intent.extras!!
        val extrasS = extras.keySet().joinToString(",") { k -> k + ":" + extras[k] }
        Log.d(TAG, "onReceive:$intent ${extrasS}")
        val bot = EnergyBot.getInstance(context)
        val next = bot.setupAlarm()

        val now = System.currentTimeMillis()
        if( EnergyBot.lastAlarm > now ){
            // NUNCA SE SABE
            EnergyBot.lastAlarm = now
        }
        val trigger = now - EnergyBot.lastAlarm - EnergyBot.alarmInterval
        if( trigger < 0 ){
            Log.d(TAG, "El tiempo que ha pasado no es suficiente para enviar una alarma por el Bot" )
            Log.d(TAG, "  lastAlarm:${EnergyBot.lastAlarm} alarmInterval:${EnergyBot.alarmInterval} now:$now trigger:$trigger" )
            return;
        }

        EnergyBot.lastAlarm = now
        val info = EnergyBot.getBatteryInfo(context)
        val msg = "⏰ Desde la alarma (próxima: $next)\n$info"
        bot.sendStatusToClients(msg)
    }
}

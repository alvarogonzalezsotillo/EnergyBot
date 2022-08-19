package agonzalez.energymonitor.bot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log


class BootReceiver : BroadcastReceiver(){
    val TAG = "BootReceiver"
    init{
        Log.d(TAG, "Receiver  creado")
    }
    override fun onReceive(contextP: Context?, intent: Intent?) {
        Log.d(TAG, "onReceive:$intent")
        val context = contextP?.applicationContext!!
        val bot = EnergyBot.getInstance(context)
        val info = EnergyBot.getBatteryInfo(context)
        val msg = "\uD83C\uDF1F Desde boot\n$info"
        bot.sendStatusToClients(msg)
    }
}
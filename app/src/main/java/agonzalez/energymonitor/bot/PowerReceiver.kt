package agonzalez.energymonitor.bot

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class PowerReceiver : BroadcastReceiver() {
    val TAG = "PowerReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        Log.d(TAG, "Receiver del bot")
        val bot = EnergyBot.getInstance(context!!)
        val previous = bot.lastBatteryInfo()
        val current = bot.registerNewBatteryInfo(true)
        if( !current.plugged && previous.plugged ) {
            val msg = "\uD83D\uDCA9 No hay corriente: $current"
            bot.sendStatusToClients(msg)
        }
        if( current.plugged && !previous.plugged ){
            val msg = "\uD83D\uDE00 Parece que ha vuelto la corriente: $current"
            bot.sendStatusToClients(msg)
        }

    }
}

package agonzalez.energymonitor.bot

import agonzalez.energymonitor.MainActivity
import android.content.BroadcastReceiver
import android.content.Context
import android.os.BatteryManager

import android.content.Intent

import android.content.IntentFilter
import android.util.Log
import android.widget.Toast
import androidx.work.*
import java.util.concurrent.TimeUnit
import androidx.work.NetworkType
import android.app.AlarmManager

import android.app.PendingIntent

import android.app.IntentService
import android.app.PendingIntent.FLAG_ONE_SHOT
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context.ALARM_SERVICE
import androidx.core.content.ContextCompat.getSystemService
import java.time.Duration


typealias BotLogger = (String) -> Unit


public class EnergyBot(context:Context) {


    val chatIds = HashSet<Long>()

    var lastBatteryInfo : BatteryInfo? = null




    val bot = PengradBot { msg ->
        Log.d(TAG,"Enviando por telegram:$msg")
        val chatId = msg.chatId
        chatIds.add(chatId)
        val response =
            "Desde energybot\n chatIds:${chatIds.toString()}\n ${msg}\n${getBatteryInfo(context)}"
        Log.d(TAG,response)
        response
    }

    init {
        Log.d(TAG,"Constructor de EnergyBot")
        registerForPowerChanges(context)
        setupAlarm(context)
        lastBatteryInfo = getBatteryInfo(context)
        chatIds += 236278140
    }

    class EnergyBootReceiver : BroadcastReceiver(){
        val TAG = "EnergyBootReceiver"
        init{
            Log.d(TAG, "Receiver  creado")
        }
        override fun onReceive(contextP: Context?, intent: Intent?) {
            Log.d(TAG,"**************************************** Ha pasado algo con boot receiver: $intent")
            Log.d(TAG,"**************************************** Ha pasado algo con boot receiver: $intent")
            Log.d(TAG,"**************************************** Ha pasado algo con boot receiver: $intent")
            Log.d(TAG,"**************************************** Ha pasado algo con boot receiver: $intent")
            Log.d(TAG,"**************************************** Ha pasado algo con boot receiver: $intent")
            Log.d(TAG,"**************************************** Ha pasado algo con boot receiver: $intent")

            Log.d(TAG, "onReceive:$intent")
            val context = contextP?.applicationContext!!
            val bot = EnergyBot.getInstance(context)
            bot.setupAlarm(context)
            val info = EnergyBot.getBatteryInfo(context)
            val msg = "\uD83C\uDF1F Desde boot\n$info"
            bot.sendStatusToClients(msg)
            bot.registerForPowerChanges(context)
        }

    }


    class EnergyAlarmReceiver : BroadcastReceiver() {
        val TAG = "EnergyAlarmReceiver"
        init{
            Log.d(TAG, "Receiver  creado")
        }
        override fun onReceive(context: Context, intent: Intent) {
            val extras = intent.extras!!
            val extrasS = extras.keySet().joinToString(",") { k -> k + ":" + extras[k] }
            Log.d(TAG, "onReceive:$intent ${extrasS}")
            val bot = EnergyBot.getInstance(context)
            bot.setupAlarm(context)
            val info = EnergyBot.getBatteryInfo(context)
            val msg = "⏰ Desde la alarma\n$info"
            bot.sendStatusToClients(msg)
            bot.registerForPowerChanges(context)
        }
    }

    val alarmInterval = Duration.ofHours(24).toMillis()
    //val alarmInterval = Duration.ofSeconds(10).toMillis()

    private fun setupAlarm(context: Context) {
        Log.d(TAG, "poniendo alarma" )
        val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, EnergyAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, FLAG_ONE_SHOT or FLAG_UPDATE_CURRENT )
        //alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, 1000, 10000, pendingIntent)
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+alarmInterval,pendingIntent)
    }


    public fun sendStatusToClients(msg: String){
        Log.d(TAG, "sendStatusToClients")
        val thread = Thread{
            Log.d(TAG, "sendStatusToClients:${this}")
            chatIds.forEach { chatId ->
                Log.d(TAG, "valor de chatId:${chatId}")
                bot.sendMessage(MessageInfo(chatId, chatId, msg))
            }
        }
        thread.start()
    }

    private fun registerForPowerChanges(context: Context) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            val TAG = "EnergyBot-powerReceiver"
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(TAG, "Receiver del bot")
                val info = getBatteryInfo(context!!)
                if( !info.plugged ) {
                    val msg =
                        "\uD83D\uDCA9 HA PASADO ALGO MALO CON LA CORRIENTE: $info"
                    sendStatusToClients(msg)
                }
                if( !lastBatteryInfo!!.plugged && info.plugged ){
                    val msg =
                        "\uD83D\uDE00 Parece que ha vuelto la corriente: $info"
                    sendStatusToClients(msg)
                }
                lastBatteryInfo = info
            }
        }

        context.applicationContext.registerReceiver(receiver, filter)
    }

    data class BatteryInfo(val percentage: Float, val charging: Boolean, val plugged: Boolean ){
        override fun toString(): String {
            return """
                percentage:$percentage
                charging: $charging
                plugged: $plugged
            """.replaceIndent("  ")
        }
    }

    companion object {

        val TAG = "EnergyBot"

        private var botInstance: EnergyBot? = null

        fun getInstance(context:Context) : EnergyBot {
            if(botInstance == null) {
                botInstance = EnergyBot(context)
            }
            return botInstance!!
        }


        fun getBatteryInfo(context: Context): BatteryInfo {
            val batteryIntent: Intent? =
                context.applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent!!.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

            val isCharging = status != BatteryManager.BATTERY_STATUS_DISCHARGING
            val isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                    plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                    plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS

            return BatteryInfo(level.toFloat() / scale.toFloat() * 100.0f, isCharging, isPlugged)
        }
    }

    fun sendMessage(msg: MessageInfo): String {
        return bot.sendMessage(msg)
    }

}





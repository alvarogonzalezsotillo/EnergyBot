package agonzalez.energymonitor.bot


import agonzalez.energymonitor.MainActivity
import android.content.BroadcastReceiver
import android.content.Context
import android.os.BatteryManager

import android.content.Intent

import android.content.IntentFilter
import android.util.Log

import android.app.AlarmManager

import android.app.PendingIntent


import android.app.PendingIntent.FLAG_ONE_SHOT
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context.ALARM_SERVICE
import android.content.Context.POWER_SERVICE
import android.os.PowerManager
import androidx.core.content.ContextCompat.getSystemService

import java.time.Duration
import java.util.*
import kotlin.collections.HashSet


typealias BotLogger = (String) -> Unit

public class EnergyBot(val context:Context) {


    var _wakeLock : PowerManager.WakeLock? = null
    private val chatIds = HashSet<Long>()

    private val MAX_BATTERYINFO = 40


    private val batteryInfoLRU = ArrayList<BatteryInfo>()


    fun registerNewBatteryInfo(filter:Boolean) : BatteryInfo {
        Log.d(TAG, "RegisterBattery $filter", Throwable())
        val ret = getBatteryInfo(context)
        if( filter && !batteryInfoLRU.isEmpty()){
            val previous = batteryInfoLRU.last()
            if( previous.plugged == ret.plugged && previous.charging == ret.charging ) {
                return ret
            }
        }
        batteryInfoLRU.add( ret )
        if( batteryInfoLRU.size > MAX_BATTERYINFO ){
            batteryInfoLRU.removeAt(0)
        }
        addToLogText("register battery:$ret")
        return ret
    }

    fun lastBatteryInfo() : BatteryInfo{
        return batteryInfoLRU.last();
    }

    val pengradBot = PengradBot { msg ->
        Log.d(TAG,"Enviando por telegram:$msg")
        val chatId = msg.chatId
        chatIds += chatId
        registerNewBatteryInfo(false)
        val response =
            """Desde energybot
               chatIds:${chatIds.toString()}
               msg: ${msg}
               Lista de información de batería:${batteryInfoLRU.joinToString("\n","\n")}
            """.replaceIndent("  ")
        Log.d(TAG,response)
        addToLogText("\n\nToClient $chatId:\n" + response)
        response
    }

    init {
        Log.d(TAG,"Constructor de EnergyBot")
        chatIds += NotAddedToGit.defaultChatId
        setup()
        sendStatusToClients("Arrancado el bot");
    }

    private fun setup(){

        registerForPowerChanges()
        setupAlarm()
        registerNewBatteryInfo(false)
        adquireWakeLock()
    }



    private fun adquireWakeLock(){
        addToLogText("comprobando wakelock")
        if( _wakeLock == null ) {
            val powerManager = context.getSystemService(POWER_SERVICE) as PowerManager
            _wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "energybot:perpetual-wake-lock"
            )
            _wakeLock!!.acquire()
            addToLogText("wakelock adquirido")
        }
    }



    fun setupAlarm() : Date {
        Log.d(TAG, "poniendo alarma" )
        val alarmManager = context.getSystemService(ALARM_SERVICE) as AlarmManager
        val alarmIntent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, 0, alarmIntent, FLAG_ONE_SHOT or FLAG_UPDATE_CURRENT )
        //alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP, 1000, 10000, pendingIntent)
        val next = Date( System.currentTimeMillis() + tick );
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,System.currentTimeMillis()+tick,pendingIntent)
        Log.d(TAG, "La alarma se ha puesto a:$next")
        return next
    }


    public fun sendStatusToClients(msg: String){
        Log.d(TAG, "sendStatusToClients")
        addToLogText("\n\nToClients:\n" + msg)
        val thread = Thread{
            Log.d(TAG, "sendStatusToClients:${this}")
            chatIds.forEach { chatId ->
                Log.d(TAG, "valor de chatId:${chatId}")
                pengradBot.sendMessage(MessageInfo(chatId, chatId, msg))
            }
        }
        thread.start()
    }

    private fun registerForPowerChanges() {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = PowerReceiver()
        context.applicationContext.registerReceiver(receiver, filter)
    }

    data class BatteryInfo(val percentage: Float, val charging: Boolean, val plugged: Boolean ){
        val date = Date()

        override fun toString(): String {
            return """
                date: $date
               percentage: $percentage
               plugged: $plugged
            """.replaceIndent("  ")
        }
    }

    companion object {

        val alarmInterval = Duration.ofMinutes(6).toMillis() //Duration.ofHours(24).toMillis()
        var lastAlarm = System.currentTimeMillis() - alarmInterval
        val tick = Duration.ofMinutes(5).toMillis()




        val TAG = "EnergyBot"

        private var botInstance: EnergyBot? = null




        fun getInstance(context:Context) : EnergyBot {
            if(botInstance == null) {
                botInstance = EnergyBot(context)
            }
            return botInstance!!
        }

        fun addToLogText(msg: String ) {
            if (MainActivity.instance != null) {
                (MainActivity.instance as MainActivity).addToLogText(msg);
            }
        }


        fun getBatteryInfo(context: Context): BatteryInfo {
            val batteryIntent: Intent? =
                context.applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
            val level = batteryIntent!!.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            val status = batteryIntent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val plugged = batteryIntent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)

            val isCharging = status != BatteryManager.BATTERY_STATUS_DISCHARGING
            val isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC ||
                    plugged == BatteryManager.BATTERY_PLUGGED_USB ||
                    plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS

            return BatteryInfo(level.toFloat() / scale.toFloat() * 100.0f, isCharging, isPlugged)
        }
    }

}





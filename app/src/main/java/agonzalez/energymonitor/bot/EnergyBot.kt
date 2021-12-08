package agonzalez.energymonitor.bot

import agonzalez.energymonitor.MainActivity
import android.content.BroadcastReceiver
import android.content.Context
import android.os.BatteryManager

import android.content.Intent

import android.content.IntentFilter
import android.util.Log
import androidx.work.*
import java.util.concurrent.TimeUnit
import androidx.work.NetworkType


typealias BotLogger = (String) -> Unit


public class EnergyBot(val context: Context, botLogger: BotLogger) {


    val chatIds = HashSet<Long>()

    var lastBatteryInfo : BatteryInfo? = null


    val bot = PengradBot { msg ->
        val chatId = msg.chatId
        chatIds.add(chatId)
        val response =
            "Desde energybot: chatIds:${chatIds.toString()} ${msg} ${getBatteryInfo(context)}"
        botLogger(response)
        response
    }

    init {
        setupWorker(context)
        registerForPowerChanges(context)
        lastBatteryInfo = getBatteryInfo(context)
    }

    private fun registerForPowerChanges(context: Context) {
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d(TAG, "Receiver del bot")
                val info = getBatteryInfo(context!!)
                if( !info.plugged ) {
                    val msg =
                        "\uD83D\uDCA9 HA PASADO ALGO MALO CON LA CORRIENTE: $info"
                    MainActivity.instance!!.sendStatusToClients(msg)
                }
                if( !lastBatteryInfo!!.plugged && info.plugged ){
                    val msg =
                        "\uD83D\uDE00 Parece que ha vuelto la corriente: $info"
                    MainActivity.instance!!.sendStatusToClients(msg)
                }
                lastBatteryInfo = info
            }
        }

        val batteryStatus = context.registerReceiver(receiver, filter)
    }

    data class BatteryInfo(val percentage: Float, val charging: Boolean, val plugged: Boolean )

    companion object {

        val TAG = "EnergyBot"

        fun getBatteryInfo(context: Context): BatteryInfo {
            val batteryIntent: Intent? =
                context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
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


    fun setupWorker(context: Context) {

        Log.d(TAG, "setupWorker")

        val constraints: Constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        val periodicRefreshRequest = PeriodicWorkRequest.Builder(
            EnergyWorker::class.java, // Your worker class
            1, // repeating interval
            TimeUnit.MINUTES,
            15, // flex interval - worker will run somewhen within this period of time, but at the end of repeating interval
            TimeUnit.SECONDS
        ).setConstraints(constraints).setInitialDelay(1, TimeUnit.MINUTES).build()

        WorkManager
            .getInstance(context)
            .enqueueUniquePeriodicWork(
                "nombreunico",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRefreshRequest
            )

        Log.d(TAG, "setupWorker terminado")
    }
}

class EnergyWorker(val appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    val TAG = "Worker"

    init {
        Log.d(TAG, "Creando UploadWorker");
    }

    override fun doWork(): Result {

        // Do the work here--in this case, upload the images.
        val info = EnergyBot.getBatteryInfo(appContext)
        val msg = "Desde el worker: $info"
        Log.d(TAG, msg)
        MainActivity.instance!!.sendStatusToClients(msg)

        // Indicate whether the work finished successfully with the Result
        return Result.success()
    }
}


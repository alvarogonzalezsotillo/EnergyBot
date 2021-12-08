package agonzalez.energymonitor

import agonzalez.energymonitor.bot.EnergyBot
import agonzalez.energymonitor.bot.MessageInfo
import androidx.appcompat.app.AppCompatActivity
import android.widget.TextView
import android.widget.ScrollView
import android.widget.ToggleButton
import android.annotation.SuppressLint
import agonzalez.energymonitor.bot.PengradBot
import android.os.Bundle
import android.os.Handler
import android.util.Log
import java.util.*
import android.os.PowerManager

import android.os.PowerManager.WakeLock




class MainActivity : AppCompatActivity() {
    val TAG = "MainActivityLogcat"
    var _logText: TextView? = null
    var _logScroll: ScrollView? = null
    var _handler: Handler? = null
    var _pauseLogButton: ToggleButton? = null
    var _wakeLock : WakeLock? = null
    var _timer = Timer(true)
    var _task: TimerTask = object : TimerTask() {
        @SuppressLint("SetTextI18n")
        override fun run() {
            if (_logText == null || _handler == null) {
                Log.d(TAG, "_logText es null")
                return
            }
            if (_pauseLogButton!!.isChecked) {
                Log.d(TAG, "log deshabilitado")
                return
            }
            val msg = EnergyBot.getBatteryInfo(this@MainActivity).toString()
            sendStatusToClients(msg)
            addToLogText("$msg\n")
        }
    }

    public fun sendStatusToClients(msg: String){
        val thread = Thread{
            val bot = getBot()
            Log.d(TAG, "valor de getBot:${bot}")
            if (bot != null) {
                val lastChatId = getBot().chatIds.forEach { chatId ->
                    Log.d(TAG, "valor de chatId:${chatId}")
                    if (chatId != null) {
                        bot.sendMessage(MessageInfo(chatId, chatId, msg))
                    }
                }
            }
        }
        thread.start()
    }

    public fun addToLogText(msg: String) {
        _handler!!.post {
            _logText!!.text = "${_logText!!.text}$msg"
            _handler!!.post {
                _logScroll!!.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }

    }


    private val _logPeriod: Long = 1000 * 60 * 24
    private var _bot: EnergyBot? = null

    fun getBot(): EnergyBot {
        if (_bot == null) {
            Log.d(TAG, "Bot no existía, creando")
            _bot = EnergyBot(this, { s -> processBotMessage(s) })
        }
        return _bot!!
    }

    private fun processBotMessage(s: String): String {
        addToLogText("Proceso mensaje:${s}")
        return "Desde android:$s"
    }

    private fun initBindings(){
        _logText = findViewById<TextView>(R.id.logText)
        _logScroll = findViewById<ScrollView>(R.id.logScroll)
        _handler = Handler()
        _pauseLogButton = findViewById<ToggleButton>(R.id.pauseLogButton)
        _timer.schedule(_task, 0, _logPeriod)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MainActivity.instance = this;
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_main)

        initBindings()
        adquireWakeLock()
    }


    private fun adquireWakeLock(){
        addToLogText("comprobando wakelock")
        if( _wakeLock == null ) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            _wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "perpetual-wake-lock"
            )
            _wakeLock!!.acquire()
            addToLogText("wakelock adquirido")
        }
    }

    companion object {
        var instance: MainActivity? = null;
    }
}
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
import android.os.Looper
import android.util.Log
import java.util.*
import android.os.PowerManager

import android.os.PowerManager.WakeLock




class MainActivity : AppCompatActivity() {
    val TAG = "MainActivityLogcat"
    var _logText: TextView? = null
    var _logScroll: ScrollView? = null
    var _handler: Handler? = null
    var _wakeLock : WakeLock? = null


    public fun addToLogText(msg: String) {
        _handler!!.post {
            _logText!!.text = "${_logText!!.text}$msg"
            _handler!!.post {
                _logScroll!!.fullScroll(ScrollView.FOCUS_DOWN)
            }
        }

    }


    private val _logPeriod: Long = 1000L * 60 * 24
    private var _bot: EnergyBot? = null


    private fun initBindings(){
        _logText = findViewById<TextView>(R.id.logText)
        _logScroll = findViewById<ScrollView>(R.id.logScroll)
        _handler = Handler(Looper.getMainLooper())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        MainActivity.instance = this;
        Log.d(TAG, "onCreate")
        setContentView(R.layout.activity_main)

        initBindings()
        adquireWakeLock()
        EnergyBot.getInstance(applicationContext)
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
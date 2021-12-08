package agonzalez.energymonitor.bot

class BotLog {
    companion object {
        fun log(msg: String) {
            println(msg)
        }
        fun log(msg: String, e: Throwable ) {
            println(msg)
            e.printStackTrace(System.out)
        }
    }
}
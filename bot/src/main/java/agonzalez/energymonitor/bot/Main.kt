package agonzalez.energymonitor.bot



fun main( args: Array<String> ){
    Bot.initBot{
        s -> "El mensaje recibido es: $s"
    }
}

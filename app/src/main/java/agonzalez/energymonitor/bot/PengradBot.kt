package agonzalez.energymonitor.bot

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.SendMessage





typealias messageHandler = (MessageInfo) -> String

data class MessageInfo(val chatId: Long, val userId: Long?, val text: String )

public class PengradBot(private val handler: messageHandler) {

    private val bot : TelegramBot = TelegramBot(NotAddedToGit.token);
    init {
        bot.setUpdatesListener{ updates ->
            updates.forEach{ update ->
                if( update == null ){
                    throw Error("Unexpected null update")
                }
                val message = update.message()
                if( message != null ) {
                    val chatId = message.chat().id()
                    val userId = message.from().id()
                    val text = message.text()
                    val response = handler( MessageInfo(chatId, userId, text) )
                    sendMessage(MessageInfo(chatId, null, response ))
                }
            }
            UpdatesListener.CONFIRMED_UPDATES_ALL
        }
    }

    fun sendMessage( msg: MessageInfo ) : String {
        val request = SendMessage(msg.chatId, msg.text)
        val sendResponse = bot.execute(request)
        val ok = sendResponse.isOk
        val message: Message? = sendResponse.message()
        return "ok:$ok message:$message"
    }
}

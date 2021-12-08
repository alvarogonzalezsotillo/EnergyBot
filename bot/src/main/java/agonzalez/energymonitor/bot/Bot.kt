package agonzalez.energymonitor.bot


import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession


typealias messageHandler = (String) -> String

class Bot (private val handler: messageHandler ): TelegramLongPollingBot() {

    companion object {
        fun initBot(handler: messageHandler) : Bot {
            val telegramBotApi = TelegramBotsApi(DefaultBotSession::class.java)
            try {
                val bot = Bot(handler)
                telegramBotApi.registerBot(bot)
                return bot
            } catch (e: TelegramApiRequestException) {
                BotLog.log("Error creating bot", e)
                throw RuntimeException("Error creating bot", e)
            }
        }
    }

    /**
     * Method for receiving messages.
     * @param update Contains a message from the user.
     */
    override fun onUpdateReceived(update: Update) {
        BotLog.log(update.toString())
        val message = update.message.text

        val response = handler(message)

        sendMsg(update.message.chatId.toString(), response)
    }

    /**
     * Method for creating a message and sending it.
     * @param chatId chat id
     * @param s The String that you want to send as a message.
     */
    @Synchronized
    fun sendMsg(chatId: String, s: String) : Boolean{
        val message = SendMessage()
        message.enableMarkdown(true)
        message.chatId = chatId
        message.text = s

        try {
            sendApiMethod(message)
        } catch (e: TelegramApiException) {
            BotLog.log("Exception: ", e)
            return false
        }
        return true
    }

    /**
     * This method returns the bot's name, which was specified during registration.
     * @return bot name
     */
    override fun getBotUsername(): String {
        return "energy_chilo_bot"
    }

    /**
     * This method returns the bot's token for communicating with the Telegram server
     * @return the bot's token
     */
    override fun getBotToken(): String {
        return "5093933942:AAH2qpCI_2CoXHZnXZOzdv6zQlRWVNoo-LE"
    }
}


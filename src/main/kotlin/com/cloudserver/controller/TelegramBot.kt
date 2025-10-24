package com.cloudserver.controller

import com.cloudserver.service.TelegramBotService
import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.command
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Controller

@Controller
class TelegramBot(
    @param:Value("\${app.telegram.token}") private val botToken: String,
    @param:Value("\${app.telegram.username}") private val botUsername: String,
    private val telegramBotService: TelegramBotService
) {
    private val logger = LoggerFactory.getLogger(TelegramBot::class.java)

    val telegramBot = bot {
        token = botToken

        dispatch {
            command("start") {
                val welcomeMessage = """
                    👋 Welcome to Crypto Arbitrage Alert Bot!
                    
                    I monitor cryptocurrency prices across Wallex and Nobitex exchanges and notify you when profitable arbitrage opportunities appear.
                    
                    *Available Commands:*
                    /subscribe - Subscribe to arbitrage alerts
                    /unsubscribe - Unsubscribe from alerts
                    /status - Check your subscription status
                    /help - Show this help message
                    
                    Get started by using /subscribe command!
                """.trimIndent()

                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = welcomeMessage,
                    parseMode = ParseMode.MARKDOWN
                )
            }

            command("help") {
                val helpMessage = """
                    📖 *Help - Crypto Arbitrage Bot*
                    
                    *Commands:*
                    /start - Show welcome message
                    /subscribe - Subscribe to arbitrage notifications
                    /unsubscribe - Stop receiving notifications
                    /status - Check your subscription status
                    /help - Show this help message
                    
                    *What is Arbitrage?*
                    Arbitrage is when you buy a cryptocurrency on one exchange at a lower price and sell it on another exchange at a higher price, making a profit from the price difference.
                    
                    *How it works:*
                    1. Subscribe using /subscribe
                    2. Receive instant notifications when opportunities appear
                    3. Execute trades quickly to capture profits
                    
                    ⚠️ *Disclaimer:* This bot provides information only. Always do your own research and trade at your own risk.
                """.trimIndent()

                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = helpMessage,
                    parseMode = ParseMode.MARKDOWN
                )
            }

            command("subscribe") {
                val chatId = message.chat.id
                val username = message.chat.username
                val firstName = message.chat.firstName
                val lastName = message.chat.lastName

                val response = telegramBotService.subscribe(chatId, username, firstName, lastName)
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = response
                )
                logger.info("User $chatId subscribed")
            }

            command("unsubscribe") {
                val chatId = message.chat.id
                val response = telegramBotService.unsubscribe(chatId)
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = response
                )
                logger.info("User $chatId unsubscribed")
            }

            command("status") {
                val chatId = message.chat.id
                val response = telegramBotService.getSubscriberStatus(chatId)
                bot.sendMessage(
                    chatId = ChatId.fromId(chatId),
                    text = response,
                    parseMode = ParseMode.MARKDOWN
                )
            }

            text {
                val responseText = """
                    ℹ️ I didn't understand that command.
                    
                    Use /help to see available commands.
                """.trimIndent()

                bot.sendMessage(
                    chatId = ChatId.fromId(message.chat.id),
                    text = responseText
                )
            }
        }
    }

    @PostConstruct
    fun startBot() {
        telegramBotService.setBot(telegramBot)
        telegramBot.startPolling()
        logger.info("Telegram bot started successfully")
    }
}
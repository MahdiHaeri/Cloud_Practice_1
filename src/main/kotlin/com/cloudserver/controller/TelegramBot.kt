package com.cloudserver.controller

import com.github.kotlintelegrambot.bot
import com.github.kotlintelegrambot.dispatch
import com.github.kotlintelegrambot.dispatcher.text
import com.github.kotlintelegrambot.entities.ChatId
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.stereotype.Controller

@Controller
class TelegramBot(
    @param:Value("\${app.telegram.token}") private val botToken: String,
    @param:Value("\${app.telegram.username}") private val botUsername: String,
) {
    val telegramBot = bot {
        token = botToken
        // Configure bot settings and handlers here
        dispatch {
            text {
                bot.sendMessage(ChatId.fromId(message.chat.id), text = text)
            }
        }
    }

    @PostConstruct
    fun startBot() {
        telegramBot.startPolling()
    }
}
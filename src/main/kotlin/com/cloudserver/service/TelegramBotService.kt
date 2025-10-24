package com.cloudserver.service

import com.cloudserver.dto.ArbitrageOpportunity
import com.cloudserver.model.TelegramSubscriber
import com.cloudserver.repository.TelegramSubscriberRepository
import com.github.kotlintelegrambot.Bot
import com.github.kotlintelegrambot.entities.ChatId
import com.github.kotlintelegrambot.entities.ParseMode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
class TelegramBotService(
    private val subscriberRepository: TelegramSubscriberRepository
) {
    private val logger = LoggerFactory.getLogger(TelegramBotService::class.java)
    private lateinit var bot: Bot

    fun setBot(bot: Bot) {
        this.bot = bot
    }

    fun subscribe(chatId: Long, username: String?, firstName: String?, lastName: String?): String {
        return try {
            val existingSubscriber = subscriberRepository.findByChatId(chatId)
            if (existingSubscriber != null) {
                if (existingSubscriber.isActive) {
                    "✅ You are already subscribed to arbitrage notifications!"
                } else {
                    existingSubscriber.isActive = true
                    subscriberRepository.save(existingSubscriber)
                    "✅ Welcome back! You have been re-subscribed to arbitrage notifications."
                }
            } else {
                val subscriber = TelegramSubscriber(
                    chatId = chatId,
                    username = username,
                    firstName = firstName,
                    lastName = lastName
                )
                subscriberRepository.save(subscriber)
                "✅ Successfully subscribed! You will receive notifications when arbitrage opportunities are found."
            }
        } catch (e: Exception) {
            logger.error("Error subscribing user $chatId", e)
            "❌ Failed to subscribe. Please try again later."
        }
    }

    fun unsubscribe(chatId: Long): String {
        return try {
            val subscriber = subscriberRepository.findByChatId(chatId)
            if (subscriber != null && subscriber.isActive) {
                subscriber.isActive = false
                subscriberRepository.save(subscriber)
                "✅ Successfully unsubscribed. You will no longer receive arbitrage notifications."
            } else {
                "ℹ️ You are not currently subscribed."
            }
        } catch (e: Exception) {
            logger.error("Error unsubscribing user $chatId", e)
            "❌ Failed to unsubscribe. Please try again later."
        }
    }

    fun getSubscriberStatus(chatId: Long): String {
        val subscriber = subscriberRepository.findByChatId(chatId)
        return if (subscriber != null && subscriber.isActive) {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            """
                📊 *Subscription Status*
                
                ✅ Status: Active
                📅 Subscribed: ${subscriber.subscribedAt.format(formatter)}
                ${if (subscriber.lastNotificationAt != null) "🔔 Last Notification: ${subscriber.lastNotificationAt!!.format(formatter)}" else ""}
            """.trimIndent()
        } else {
            "ℹ️ You are not currently subscribed. Use /subscribe to start receiving notifications."
        }
    }

    fun sendArbitrageAlert(opportunity: ArbitrageOpportunity) {
        val activeSubscribers = subscriberRepository.findAllByIsActiveTrue()
        logger.info("Sending arbitrage alert to ${activeSubscribers.size} subscribers")

        val message = formatArbitrageMessage(opportunity)

        activeSubscribers.forEach { subscriber ->
            try {
                bot.sendMessage(
                    chatId = ChatId.fromId(subscriber.chatId),
                    text = message,
                    parseMode = ParseMode.MARKDOWN
                )
                subscriber.lastNotificationAt = LocalDateTime.now()
                subscriberRepository.save(subscriber)
                logger.debug("Alert sent to subscriber ${subscriber.chatId}")
            } catch (e: Exception) {
                logger.error("Failed to send alert to subscriber ${subscriber.chatId}", e)
            }
        }
    }

    private fun formatArbitrageMessage(opportunity: ArbitrageOpportunity): String {
        val timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))

        return buildString {
            appendLine("🚨 *ARBITRAGE OPPORTUNITY DETECTED!* 🚨")
            appendLine()
            appendLine("💰 *Market:* ${opportunity.symbol}")
            appendLine("⏰ *Time:* $timestamp")
            appendLine()

            opportunity.opportunities.forEach { detail ->
                appendLine("📈 *Strategy:*")
                appendLine("   🟢 Buy from: *${detail.buyExchange}*")
                appendLine("   💵 Buy Price: `${formatPrice(detail.buyPrice)} TMN`")
                appendLine()
                appendLine("   🔴 Sell on: *${detail.sellExchange}*")
                appendLine("   💵 Sell Price: `${formatPrice(detail.sellPrice)} TMN`")
                appendLine()
                appendLine("   ✨ *Profit: ${formatPrice(detail.profit)} TMN*")
                appendLine("   📊 *Profit %: ${formatPercentage(detail.profitPercentage)}%*")
                appendLine()
            }

            appendLine("⚠️ _Note: Execute trades quickly as opportunities may disappear fast!_")
        }
    }

    private fun formatPrice(price: java.math.BigDecimal): String {
        return String.format("%,.2f", price)
    }

    private fun formatPercentage(percentage: java.math.BigDecimal): String {
        return String.format("%.4f", percentage)
    }

    fun getActiveSubscriberCount(): Int {
        return subscriberRepository.findAllByIsActiveTrue().size
    }
}

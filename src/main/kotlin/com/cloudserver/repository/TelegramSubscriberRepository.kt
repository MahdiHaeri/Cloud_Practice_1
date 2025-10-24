package com.cloudserver.repository

import com.cloudserver.model.TelegramSubscriber
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface TelegramSubscriberRepository : JpaRepository<TelegramSubscriber, Long> {
    fun findByChatId(chatId: Long): TelegramSubscriber?
    fun findAllByIsActiveTrue(): List<TelegramSubscriber>
    fun existsByChatId(chatId: Long): Boolean
}


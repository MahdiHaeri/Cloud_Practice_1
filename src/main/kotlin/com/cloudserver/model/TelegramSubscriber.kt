package com.cloudserver.model

import jakarta.persistence.*
import java.time.LocalDateTime

@Entity
@Table(name = "telegram_subscribers")
data class TelegramSubscriber(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(unique = true, nullable = false)
    val chatId: Long,

    @Column(nullable = false)
    val username: String? = null,

    @Column(nullable = false)
    val firstName: String? = null,

    @Column
    val lastName: String? = null,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @Column(nullable = false)
    val subscribedAt: LocalDateTime = LocalDateTime.now(),

    @Column
    var lastNotificationAt: LocalDateTime? = null
)


package com.cloudserver.model

import jakarta.persistence.*
import java.math.BigDecimal
import java.time.LocalDateTime

@Entity
@Table(name = "exchange_prices")
data class ExchangePrice(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false)
    val exchange: String, // "Wallex" or "Nobitex"

    @Column(nullable = false)
    val symbol: String, // e.g., "BTCTMN", "BTCIRT"

    @Column(nullable = false)
    val asset: String, // e.g., "BTC", "ETH"

    @Column(nullable = false)
    val quoteCurrency: String, // "TMN" or "IRT"

    @Column(precision = 20, scale = 8)
    val bidPrice: BigDecimal?, // Best bid price

    @Column(precision = 20, scale = 8)
    val askPrice: BigDecimal?, // Best ask price

    @Column(precision = 20, scale = 8)
    val lastPrice: BigDecimal?, // Last trade price

    @Column(nullable = false)
    val timestamp: LocalDateTime = LocalDateTime.now(),

    @Column
    val responseTimeMs: Long? = null // API response time in milliseconds
)


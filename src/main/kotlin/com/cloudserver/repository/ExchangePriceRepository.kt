package com.cloudserver.repository

import com.cloudserver.model.ExchangePrice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
interface ExchangePriceRepository : JpaRepository<ExchangePrice, Long> {

    // Find latest prices for a specific exchange and symbol
    fun findTopByExchangeAndSymbolOrderByTimestampDesc(exchange: String, symbol: String): ExchangePrice?

    // Find all prices for a specific exchange within a time range
    fun findByExchangeAndTimestampBetween(
        exchange: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<ExchangePrice>

    // Find all prices for a specific asset
    fun findByAssetOrderByTimestampDesc(asset: String): List<ExchangePrice>
}


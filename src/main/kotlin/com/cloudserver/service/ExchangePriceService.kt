package com.cloudserver.service

import com.cloudserver.model.ExchangePrice
import com.cloudserver.repository.ExchangePriceRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime

@Service
class ExchangePriceService(
    private val exchangePriceRepository: ExchangePriceRepository
) {

    private val logger = LoggerFactory.getLogger(ExchangePriceService::class.java)

    /**
     * Save exchange price data to database
     */
    @Transactional
    fun saveExchangePrice(
        exchange: String,
        symbol: String,
        asset: String,
        quoteCurrency: String,
        bidPrice: BigDecimal?,
        askPrice: BigDecimal?,
        lastPrice: BigDecimal?,
        responseTimeMs: Long? = null
    ): ExchangePrice {
        val exchangePrice = ExchangePrice(
            exchange = exchange,
            symbol = symbol,
            asset = asset,
            quoteCurrency = quoteCurrency,
            bidPrice = bidPrice,
            askPrice = askPrice,
            lastPrice = lastPrice,
            timestamp = LocalDateTime.now(),
            responseTimeMs = responseTimeMs
        )

        val saved = exchangePriceRepository.save(exchangePrice)
        logger.debug("Saved price data for $exchange - $symbol: bid=$bidPrice, ask=$askPrice, last=$lastPrice")
        return saved
    }

    /**
     * Get latest price for a specific exchange and symbol
     */
    fun getLatestPrice(exchange: String, symbol: String): ExchangePrice? {
        return exchangePriceRepository.findTopByExchangeAndSymbolOrderByTimestampDesc(exchange, symbol)
    }

    /**
     * Get price history for an asset
     */
    fun getPriceHistory(asset: String): List<ExchangePrice> {
        return exchangePriceRepository.findByAssetOrderByTimestampDesc(asset)
    }

    /**
     * Get price history for an exchange within a time range
     */
    fun getPriceHistoryByTimeRange(
        exchange: String,
        startTime: LocalDateTime,
        endTime: LocalDateTime
    ): List<ExchangePrice> {
        return exchangePriceRepository.findByExchangeAndTimestampBetween(exchange, startTime, endTime)
    }
}


package com.cloudserver.controller

import com.cloudserver.model.ExchangePrice
import com.cloudserver.service.ExchangePriceService
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/prices")
class ExchangePriceController(
    private val exchangePriceService: ExchangePriceService
) {

    /**
     * Get latest price for a specific exchange and symbol
     * Example: GET /api/prices/latest?exchange=Wallex&symbol=BTCTMN
     */
    @GetMapping("/latest")
    fun getLatestPrice(
        @RequestParam exchange: String,
        @RequestParam symbol: String
    ): ResponseEntity<ExchangePrice> {
        val price = exchangePriceService.getLatestPrice(exchange, symbol)
        return if (price != null) {
            ResponseEntity.ok(price)
        } else {
            ResponseEntity.notFound().build()
        }
    }

    /**
     * Get price history for a specific asset
     * Example: GET /api/prices/history/BTC
     */
    @GetMapping("/history/{asset}")
    fun getPriceHistory(@PathVariable asset: String): ResponseEntity<List<ExchangePrice>> {
        val history = exchangePriceService.getPriceHistory(asset)
        return ResponseEntity.ok(history)
    }

    /**
     * Get price history for an exchange within a time range
     * Example: GET /api/prices/range?exchange=Wallex&startTime=2025-10-24T00:00:00&endTime=2025-10-24T23:59:59
     */
    @GetMapping("/range")
    fun getPriceHistoryByTimeRange(
        @RequestParam exchange: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) startTime: LocalDateTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) endTime: LocalDateTime
    ): ResponseEntity<List<ExchangePrice>> {
        val history = exchangePriceService.getPriceHistoryByTimeRange(exchange, startTime, endTime)
        return ResponseEntity.ok(history)
    }
}


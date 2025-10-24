package com.cloudserver.service

import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

@Service
class MetricsService(private val meterRegistry: MeterRegistry) {

    // Request counters for each exchange
    private val wallexSuccessCounter: Counter = Counter.builder("exchange.requests.success")
        .tag("exchange", "wallex")
        .description("Total successful requests to Wallex exchange")
        .register(meterRegistry)

    private val wallexFailureCounter: Counter = Counter.builder("exchange.requests.failure")
        .tag("exchange", "wallex")
        .description("Total failed requests to Wallex exchange")
        .register(meterRegistry)

    private val nobitexSuccessCounter: Counter = Counter.builder("exchange.requests.success")
        .tag("exchange", "nobitex")
        .description("Total successful requests to Nobitex exchange")
        .register(meterRegistry)

    private val nobitexFailureCounter: Counter = Counter.builder("exchange.requests.failure")
        .tag("exchange", "nobitex")
        .description("Total failed requests to Nobitex exchange")
        .register(meterRegistry)

    // Response time timers
    private val wallexTimer: Timer = Timer.builder("exchange.response.time")
        .tag("exchange", "wallex")
        .description("Response time for Wallex API calls")
        .register(meterRegistry)

    private val nobitexTimer: Timer = Timer.builder("exchange.response.time")
        .tag("exchange", "nobitex")
        .description("Response time for Nobitex API calls")
        .register(meterRegistry)

    // Arbitrage discovery metrics
    private val arbitrageDiscoveryCounter: Counter = Counter.builder("arbitrage.opportunities.discovered")
        .description("Total number of arbitrage opportunities discovered")
        .register(meterRegistry)

    private val arbitrageCheckCounter: Counter = Counter.builder("arbitrage.checks.total")
        .description("Total number of arbitrage checks performed")
        .register(meterRegistry)

    // Latest spread gauges
    private val latestWallexBidPrice = AtomicReference<BigDecimal>(BigDecimal.ZERO)
    private val latestWallexAskPrice = AtomicReference<BigDecimal>(BigDecimal.ZERO)
    private val latestNobitexBidPrice = AtomicReference<BigDecimal>(BigDecimal.ZERO)
    private val latestNobitexAskPrice = AtomicReference<BigDecimal>(BigDecimal.ZERO)
    private val latestSpreadPercentage = AtomicReference<BigDecimal>(BigDecimal.ZERO)
    private val latestProfitAmount = AtomicReference<BigDecimal>(BigDecimal.ZERO)

    // Discovery rate tracking
    private val lastDiscoveryTime = AtomicLong(System.currentTimeMillis())
    private val opportunitiesInLastMinute = AtomicLong(0)

    init {
        // Register gauges for latest prices
        Gauge.builder("exchange.latest.bid.price", latestWallexBidPrice) { it.get().toDouble() }
            .tag("exchange", "wallex")
            .description("Latest bid price from Wallex")
            .register(meterRegistry)

        Gauge.builder("exchange.latest.ask.price", latestWallexAskPrice) { it.get().toDouble() }
            .tag("exchange", "wallex")
            .description("Latest ask price from Wallex")
            .register(meterRegistry)

        Gauge.builder("exchange.latest.bid.price", latestNobitexBidPrice) { it.get().toDouble() }
            .tag("exchange", "nobitex")
            .description("Latest bid price from Nobitex")
            .register(meterRegistry)

        Gauge.builder("exchange.latest.ask.price", latestNobitexAskPrice) { it.get().toDouble() }
            .tag("exchange", "nobitex")
            .description("Latest ask price from Nobitex")
            .register(meterRegistry)

        // Register gauges for spread metrics
        Gauge.builder("arbitrage.latest.spread.percentage", latestSpreadPercentage) { it.get().toDouble() }
            .description("Latest observed spread percentage")
            .register(meterRegistry)

        Gauge.builder("arbitrage.latest.profit.amount", latestProfitAmount) { it.get().toDouble() }
            .description("Latest observed profit amount in TMN")
            .register(meterRegistry)

        // Discovery rate (opportunities per minute)
        Gauge.builder("arbitrage.discovery.rate", opportunitiesInLastMinute) { it.get().toDouble() }
            .description("Number of arbitrage opportunities discovered in the last minute")
            .register(meterRegistry)
    }

    // Record successful exchange request
    fun recordWallexSuccess() {
        wallexSuccessCounter.increment()
    }

    fun recordNobitexSuccess() {
        nobitexSuccessCounter.increment()
    }

    // Record failed exchange request
    fun recordWallexFailure() {
        wallexFailureCounter.increment()
    }

    fun recordNobitexFailure() {
        nobitexFailureCounter.increment()
    }

    // Record response time in nanoseconds
    fun recordWallexResponseTime(durationNanos: Long) {
        wallexTimer.record(Duration.ofNanos(durationNanos))
    }

    fun recordNobitexResponseTime(durationNanos: Long) {
        nobitexTimer.record(Duration.ofNanos(durationNanos))
    }

    // Record arbitrage check
    fun recordArbitrageCheck() {
        arbitrageCheckCounter.increment()
    }

    // Record arbitrage discovery
    fun recordArbitrageDiscovery(
        wallexBid: BigDecimal?,
        wallexAsk: BigDecimal?,
        nobitexBid: BigDecimal?,
        nobitexAsk: BigDecimal?,
        spreadPercentage: BigDecimal,
        profitAmount: BigDecimal
    ) {
        arbitrageDiscoveryCounter.increment()

        // Update latest prices
        wallexBid?.let { latestWallexBidPrice.set(it) }
        wallexAsk?.let { latestWallexAskPrice.set(it) }
        nobitexBid?.let { latestNobitexBidPrice.set(it) }
        nobitexAsk?.let { latestNobitexAskPrice.set(it) }

        // Update spread metrics
        latestSpreadPercentage.set(spreadPercentage)
        latestProfitAmount.set(profitAmount)

        // Update discovery rate
        updateDiscoveryRate()
    }

    // Update latest prices even when no opportunity is found
    fun updateLatestPrices(
        wallexBid: BigDecimal?,
        wallexAsk: BigDecimal?,
        nobitexBid: BigDecimal?,
        nobitexAsk: BigDecimal?
    ) {
        wallexBid?.let { latestWallexBidPrice.set(it) }
        wallexAsk?.let { latestWallexAskPrice.set(it) }
        nobitexBid?.let { latestNobitexBidPrice.set(it) }
        nobitexAsk?.let { latestNobitexAskPrice.set(it) }
    }

    private fun updateDiscoveryRate() {
        val currentTime = System.currentTimeMillis()
        val lastTime = lastDiscoveryTime.get()

        // Reset counter if more than 1 minute has passed
        if (currentTime - lastTime > 60000) {
            opportunitiesInLastMinute.set(1)
            lastDiscoveryTime.set(currentTime)
        } else {
            opportunitiesInLastMinute.incrementAndGet()
        }
    }

    // Get current metrics summary
    fun getMetricsSummary(): Map<String, Any> {
        return mapOf(
            "wallex" to mapOf(
                "successful_requests" to wallexSuccessCounter.count(),
                "failed_requests" to wallexFailureCounter.count(),
                "latest_bid" to latestWallexBidPrice.get(),
                "latest_ask" to latestWallexAskPrice.get()
            ),
            "nobitex" to mapOf(
                "successful_requests" to nobitexSuccessCounter.count(),
                "failed_requests" to nobitexFailureCounter.count(),
                "latest_bid" to latestNobitexBidPrice.get(),
                "latest_ask" to latestNobitexAskPrice.get()
            ),
            "arbitrage" to mapOf(
                "total_checks" to arbitrageCheckCounter.count(),
                "opportunities_discovered" to arbitrageDiscoveryCounter.count(),
                "discovery_rate_per_minute" to opportunitiesInLastMinute.get(),
                "latest_spread_percentage" to latestSpreadPercentage.get(),
                "latest_profit_amount" to latestProfitAmount.get()
            )
        )
    }
}


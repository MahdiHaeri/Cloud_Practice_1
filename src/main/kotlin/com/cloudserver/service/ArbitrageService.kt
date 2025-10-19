package com.cloudserver.service

import com.cloudserver.enums.TokenEnum
import com.cloudserver.service.external.NobitexService
import com.cloudserver.service.external.WallexService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.math.RoundingMode

@Service
class ArbitrageService(
    private val wallexService: WallexService,
    private val nobitexService: NobitexService
) {

    private val logger = LoggerFactory.getLogger(ArbitrageService::class.java)

    /**
     * Calculate arbitrage opportunity between Wallex and Nobitex
     * Returns the percentage difference and which exchange to buy/sell from
     */
    fun calculateArbitrage(source: TokenEnum, destination: TokenEnum): Mono<ArbitrageOpportunity> {
        logger.info("Calculating arbitrage for $source/$destination between Wallex and Nobitex")

        val wallexData = wallexService.getMarkets()
        val nobitexData = nobitexService.getOrderbook("${source}${destination}")

        return Mono.zip(wallexData, nobitexData) { wallex, nobitex ->
            val symbol = "${source}${destination}".lowercase()

            // Get Wallex prices
            val wallexBidPrice = wallex.result?.symbols?.get(symbol)?.stats?.bidPrice?.toBigDecimalOrNull()
            val wallexAskPrice = wallex.result?.symbols?.get(symbol)?.stats?.askPrice?.toBigDecimalOrNull()

            // Get Nobitex prices
            val nobitexBidPrice = nobitex.bids?.firstOrNull()?.get(0)?.toBigDecimalOrNull()
            val nobitexAskPrice = nobitex.asks?.firstOrNull()?.get(0)?.toBigDecimalOrNull()

            logger.info("Wallex - Bid: $wallexBidPrice, Ask: $wallexAskPrice")
            logger.info("Nobitex - Bid: $nobitexBidPrice, Ask: $nobitexAskPrice")

            // Calculate arbitrage opportunities
            val opportunities = mutableListOf<ArbitrageDetail>()

            // Opportunity 1: Buy from Wallex, Sell on Nobitex
            if (wallexAskPrice != null && nobitexBidPrice != null && nobitexBidPrice > wallexAskPrice) {
                val profit = nobitexBidPrice.subtract(wallexAskPrice)
                val profitPercentage = profit.divide(wallexAskPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100))
                opportunities.add(
                    ArbitrageDetail(
                        buyExchange = "Wallex",
                        sellExchange = "Nobitex",
                        buyPrice = wallexAskPrice,
                        sellPrice = nobitexBidPrice,
                        profitPercentage = profitPercentage,
                        profit = profit
                    )
                )
            }

            // Opportunity 2: Buy from Nobitex, Sell on Wallex
            if (nobitexAskPrice != null && wallexBidPrice != null && wallexBidPrice > nobitexAskPrice) {
                val profit = wallexBidPrice.subtract(nobitexAskPrice)
                val profitPercentage = profit.divide(nobitexAskPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100))
                opportunities.add(
                    ArbitrageDetail(
                        buyExchange = "Nobitex",
                        sellExchange = "Wallex",
                        buyPrice = nobitexAskPrice,
                        sellPrice = wallexBidPrice,
                        profitPercentage = profitPercentage,
                        profit = profit
                    )
                )
            }

            ArbitrageOpportunity(
                symbol = "${source}${destination}",
                opportunities = opportunities,
                hasOpportunity = opportunities.isNotEmpty()
            )
        }.doOnError { error ->
            logger.error("Error calculating arbitrage", error)
        }
    }
}

data class ArbitrageOpportunity(
    val symbol: String,
    val opportunities: List<ArbitrageDetail>,
    val hasOpportunity: Boolean
)

data class ArbitrageDetail(
    val buyExchange: String,
    val sellExchange: String,
    val buyPrice: BigDecimal,
    val sellPrice: BigDecimal,
    val profitPercentage: BigDecimal,
    val profit: BigDecimal
)


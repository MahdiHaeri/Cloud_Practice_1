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
     * Both exchanges quote assets in Iranian currency:
     * - Wallex uses TMN (Toman)
     * - Nobitex uses IRT (Iranian Rial)
     * Note: 1 Toman = 10 Rials
     */
    fun calculateArbitrage(asset: TokenEnum): Mono<ArbitrageOpportunity> {
        logger.info("Calculating arbitrage for $asset between Wallex and Nobitex")

        val wallexData = wallexService.getMarkets()
        val wallexSymbol = "${asset}TMN"

        // Nobitex uses IRT suffix for Iranian Rial
        val nobitexSymbol = "${asset}IRT"
        val nobitexData = nobitexService.getOrderbook(nobitexSymbol)

        return Mono.zip(wallexData, nobitexData) { wallex, nobitex ->
            // Get Wallex prices - find the market by symbol
            val wallexMarket = wallex.result?.markets?.find { it.symbol?.equals(wallexSymbol, ignoreCase = true) == true }
            val wallexBidPriceToman = wallexMarket?.fairPrice?.bid?.toBigDecimalOrNull() ?: wallexMarket?.price?.toBigDecimalOrNull()
            val wallexAskPriceToman = wallexMarket?.fairPrice?.ask?.toBigDecimalOrNull() ?: wallexMarket?.price?.toBigDecimalOrNull()

            // Get Nobitex prices (in Rials - IRT)
            val nobitexBidPriceRial = nobitex.bids?.firstOrNull()?.get(0)?.toBigDecimalOrNull()
            val nobitexAskPriceRial = nobitex.asks?.firstOrNull()?.get(0)?.toBigDecimalOrNull()

            // Convert Nobitex prices from Rial (IRT) to Toman (divide by 10)
            val nobitexBidPrice = nobitexBidPriceRial?.divide(BigDecimal(10), 2, RoundingMode.HALF_UP)
            val nobitexAskPrice = nobitexAskPriceRial?.divide(BigDecimal(10), 2, RoundingMode.HALF_UP)

            logger.info("Wallex ($wallexSymbol) - Bid: $wallexBidPriceToman TMN, Ask: $wallexAskPriceToman TMN")
            logger.info("Nobitex ($nobitexSymbol) - Bid: $nobitexBidPrice TMN (${nobitexBidPriceRial} IRT), Ask: $nobitexAskPrice TMN (${nobitexAskPriceRial} IRT)")

            // Calculate arbitrage opportunities
            val opportunities = mutableListOf<ArbitrageDetail>()

            // Opportunity 1: Buy from Wallex, Sell on Nobitex
            if (wallexAskPriceToman != null && nobitexBidPrice != null && nobitexBidPrice > wallexAskPriceToman) {
                val profit = nobitexBidPrice.subtract(wallexAskPriceToman)
                val profitPercentage = profit.divide(wallexAskPriceToman, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100))
                opportunities.add(
                    ArbitrageDetail(
                        buyExchange = "Wallex",
                        sellExchange = "Nobitex",
                        buyPrice = wallexAskPriceToman,
                        sellPrice = nobitexBidPrice,
                        profitPercentage = profitPercentage,
                        profit = profit
                    )
                )
            }

            // Opportunity 2: Buy from Nobitex, Sell on Wallex
            if (nobitexAskPrice != null && wallexBidPriceToman != null && wallexBidPriceToman > nobitexAskPrice) {
                val profit = wallexBidPriceToman.subtract(nobitexAskPrice)
                val profitPercentage = profit.divide(nobitexAskPrice, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal(100))
                opportunities.add(
                    ArbitrageDetail(
                        buyExchange = "Nobitex",
                        sellExchange = "Wallex",
                        buyPrice = nobitexAskPrice,
                        sellPrice = wallexBidPriceToman,
                        profitPercentage = profitPercentage,
                        profit = profit
                    )
                )
            }

            ArbitrageOpportunity(
                symbol = "$asset/TMN",
                opportunities = opportunities,
                hasOpportunity = opportunities.isNotEmpty()
            )
        }.doOnError { error ->
            logger.error("Error calculating arbitrage for $asset", error)
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

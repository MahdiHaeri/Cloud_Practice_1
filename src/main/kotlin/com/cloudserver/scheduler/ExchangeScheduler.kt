package com.cloudserver.scheduler

import com.cloudserver.enums.TokenEnum
import com.cloudserver.service.ArbitrageService
import com.cloudserver.service.external.NobitexService
import com.cloudserver.service.external.WallexService
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ExchangeScheduler(
    private val nobitexService: NobitexService,
    private val wallexService: WallexService,
    private val arbitrageService: ArbitrageService
) {

    private val logger = LoggerFactory.getLogger(ExchangeScheduler::class.java)

    @Scheduled(fixedDelay = 1000)
    fun getExchangePrice() {
        // Calculate and log arbitrage opportunities for BTC in TMN/IRT
        arbitrageService.calculateArbitrage(TokenEnum.BTC)
            .subscribe { opportunity ->
                if (opportunity.hasOpportunity) {
                    logger.info("🚨 ARBITRAGE OPPORTUNITY FOUND for ${opportunity.symbol}!")
                    opportunity.opportunities.forEach { detail ->
                        logger.info("  💰 Buy from ${detail.buyExchange} at ${detail.buyPrice} TMN, Sell on ${detail.sellExchange} at ${detail.sellPrice} TMN")
                        logger.info("  📈 Profit: ${detail.profit} TMN (${detail.profitPercentage}%)")
                    }
                } else {
                    logger.info("No arbitrage opportunity for ${opportunity.symbol}")
                }
            }
    }
}
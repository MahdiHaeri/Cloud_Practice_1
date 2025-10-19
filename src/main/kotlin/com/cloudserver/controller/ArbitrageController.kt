package com.cloudserver.controller

import com.cloudserver.enums.TokenEnum
import com.cloudserver.service.ArbitrageService
import com.cloudserver.service.ArbitrageOpportunity
import org.springframework.web.bind.annotation.*
import reactor.core.publisher.Mono

@RestController
@RequestMapping("/api/arbitrage")
class ArbitrageController(
    private val arbitrageService: ArbitrageService
) {

    /**
     * Get arbitrage opportunities for a specific token pair
     * Example: GET /api/arbitrage/BTC/IRT
     */
    @GetMapping("/{source}/{destination}")
    fun getArbitrage(
        @PathVariable source: String,
        @PathVariable destination: String
    ): Mono<ArbitrageOpportunity> {
        val sourceToken = TokenEnum.valueOf(source.uppercase())
        val destinationToken = TokenEnum.valueOf(destination.uppercase())
        return arbitrageService.calculateArbitrage(sourceToken, destinationToken)
    }
}


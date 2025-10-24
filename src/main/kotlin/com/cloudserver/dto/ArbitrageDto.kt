package com.cloudserver.dto

import java.math.BigDecimal

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


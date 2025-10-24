package com.cloudserver.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class WallexMarketResponse(
    val result: WallexResult?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WallexResult(
    val markets: List<WallexMarket>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WallexMarket(
    val symbol: String?,
    @JsonProperty("base_asset")
    val baseAsset: String?,
    @JsonProperty("quote_asset")
    val quoteAsset: String?,
    val price: String?,
    @JsonProperty("change_24h")
    val change24h: Double?,
    @JsonProperty("volume_24h")
    val volume24h: Double?,
    @JsonProperty("fair_price")
    val fairPrice: WallexFairPrice?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WallexFairPrice(
    val ask: String?,
    val bid: String?,
    val threshold: String?
)

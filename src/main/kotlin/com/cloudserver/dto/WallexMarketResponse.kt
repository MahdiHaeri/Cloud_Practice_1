package com.cloudserver.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class WallexMarketResponse(
    val result: WallexResult?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WallexResult(
    val symbols: Map<String, WallexSymbol>?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WallexSymbol(
    val symbol: String?,
    @JsonProperty("stats")
    val stats: WallexStats?,
    @JsonProperty("price")
    val price: WallexPrice?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WallexStats(
    @JsonProperty("bidPrice")
    val bidPrice: String?,
    @JsonProperty("askPrice")
    val askPrice: String?,
    @JsonProperty("24h_ch")
    val change24h: String?,
    @JsonProperty("7d_ch")
    val change7d: String?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WallexPrice(
    val min: String?,
    val max: String?,
    val change: WallexChange?
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WallexChange(
    val percentage: String?,
    val value: String?
)


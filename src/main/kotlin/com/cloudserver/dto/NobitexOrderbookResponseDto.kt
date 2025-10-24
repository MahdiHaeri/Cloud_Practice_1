package com.cloudserver.dto

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

@JsonIgnoreProperties(ignoreUnknown = true)
data class NobitexOrderbookResponse(
    val status: String?,
    val bids: List<List<String>>?,
    val asks: List<List<String>>?,
    @JsonProperty("lastUpdate")
    val lastUpdate: Long?,
    @JsonProperty("lastTradePrice")
    val lastTradePrice: String?
)


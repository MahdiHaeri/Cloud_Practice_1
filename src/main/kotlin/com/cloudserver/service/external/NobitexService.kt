package com.cloudserver.service.external

import com.cloudserver.dto.NobitexOrderbookResponse
import com.cloudserver.enums.TokenEnum
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class NobitexService(
    private val webClientBuilder:  WebClient.Builder
) : ExternalExchange {

    private val logger = LoggerFactory.getLogger(NobitexService::class.java)

    private val domain = "https://apiv2.nobitex.ir"
    private val webClient = webClientBuilder.baseUrl(domain).build()


    override fun getExchangePrice(source: TokenEnum, destination: TokenEnum, precision: Int) {
        logger.info("Getting exchange price for $source to $destination")
        try {
            // Nobitex API expects symbols in format like BTCIRT, ETHIRT, USDTIRT
            // For crypto pairs, the quote currency should be IRT (Iranian Rial) or similar
            val symbol = "${destination}${source}"  // Reversed order: destination first, then source
            val response = getOrderbook(symbol).block()
            response?.let { orderbook ->
                val bestBid = orderbook.bids?.firstOrNull()?.get(0)
                val bestAsk = orderbook.asks?.firstOrNull()?.get(0)
                logger.info("Orderbook for $symbol: bestBid=$bestBid, bestAsk=$bestAsk, lastTradePrice=${orderbook.lastTradePrice}")
            }
        } catch (e: Exception) {
            logger.error("Error fetching orderbook from Nobitex", e)
        }
    }

    /**
     * Get orderbook data for a specific symbol from Nobitex
     * API: GET /v3/orderbook/{symbol}
     * Example: BTCIRT, ETHIRT, USDTIRT
     */
    fun getOrderbook(symbol: String): Mono<NobitexOrderbookResponse> {
        logger.debug("Fetching orderbook for symbol: $symbol from Nobitex")
        return webClient.get()
            .uri("/v3/orderbook/$symbol")
            .retrieve()
            .bodyToMono(NobitexOrderbookResponse::class.java)
            .doOnError { error ->
                logger.error("Error calling Nobitex orderbook API for symbol: $symbol", error)
            }
    }

    /**
     * Get orderbook for a token pair
     */
    fun getOrderbookForPair(source: TokenEnum, destination: TokenEnum): Mono<NobitexOrderbookResponse> {
        val symbol = "${source}${destination}"
        return getOrderbook(symbol)
    }
}
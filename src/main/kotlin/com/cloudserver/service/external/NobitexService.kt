package com.cloudserver.service.external

import com.cloudserver.dto.NobitexOrderbookResponseDto
import com.cloudserver.enums.TokenEnum
import com.cloudserver.service.MetricsService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class NobitexService(
    private val webClientBuilder: WebClient.Builder,
    private val metricsService: MetricsService
) : ExternalExchange {

    private val logger = LoggerFactory.getLogger(NobitexService::class.java)

    private val domain = "https://apiv2.nobitex.ir"
    private val webClient = webClientBuilder.baseUrl(domain).build()

    override fun getExchangePrice(source: TokenEnum, destination: TokenEnum, precision: Int) {
        logger.info("Getting exchange price for $source to $destination")
        try {
            val symbol = "${destination}${source}"
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

    fun getOrderbook(symbol: String): Mono<NobitexOrderbookResponseDto> {
        logger.debug("Fetching orderbook for symbol: $symbol from Nobitex")
        val startTime = System.nanoTime()

        return webClient.get()
            .uri("/v3/orderbook/$symbol")
            .retrieve()
            .bodyToMono(NobitexOrderbookResponseDto::class.java)
            .doOnSuccess { response ->
                val duration = System.nanoTime() - startTime
                metricsService.recordNobitexResponseTime(duration)
                metricsService.recordNobitexSuccess()
                logger.debug("Successfully fetched Nobitex orderbook for $symbol in ${duration / 1_000_000}ms")
            }
            .doOnError { error ->
                val duration = System.nanoTime() - startTime
                metricsService.recordNobitexResponseTime(duration)
                metricsService.recordNobitexFailure()
                logger.error("Error calling Nobitex orderbook API for symbol: $symbol after ${duration / 1_000_000}ms", error)
            }
    }

    /**
     * Get orderbook for a token pair
     */
    fun getOrderbookForPair(source: TokenEnum, destination: TokenEnum): Mono<NobitexOrderbookResponseDto> {
        val symbol = "${source}${destination}"
        return getOrderbook(symbol)
    }
}
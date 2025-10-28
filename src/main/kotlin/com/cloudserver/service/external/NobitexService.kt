package com.cloudserver.service.external

import com.cloudserver.dto.NobitexOrderbookResponseDto
import com.cloudserver.enums.TokenEnum
import com.cloudserver.service.ExchangePriceService
import com.cloudserver.service.MetricsService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class NobitexService(
    private val webClientBuilder: WebClient.Builder,
    private val metricsService: MetricsService,
    private val exchangePriceService: ExchangePriceService
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
                val durationMs = duration / 1_000_000
                metricsService.recordNobitexResponseTime(duration)
                metricsService.recordNobitexSuccess()
                logger.debug("Successfully fetched Nobitex orderbook for $symbol in ${durationMs}ms")

                // Extract price data and save to database
                try {
                    val bestBid = response.bids?.firstOrNull()?.get(0)?.toBigDecimalOrNull()
                    val bestAsk = response.asks?.firstOrNull()?.get(0)?.toBigDecimalOrNull()
                    val lastPrice = response.lastTradePrice?.toBigDecimalOrNull()

                    // Extract asset from symbol (e.g., "BTCIRT" -> "BTC")
                    val asset = symbol.replace("IRT", "").replace("USDT", "")

                    exchangePriceService.saveExchangePrice(
                        exchange = "Nobitex",
                        symbol = symbol,
                        asset = asset,
                        quoteCurrency = "IRT",
                        bidPrice = bestBid,
                        askPrice = bestAsk,
                        lastPrice = lastPrice,
                        responseTimeMs = durationMs
                    )
                } catch (e: Exception) {
                    logger.error("Error saving Nobitex price to database", e)
                }
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
package com.cloudserver.service.external

import com.cloudserver.dto.WallexMarketResponse
import com.cloudserver.enums.TokenEnum
import com.cloudserver.service.ExchangePriceService
import com.cloudserver.service.MetricsService
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class WallexService(
    private val webClientBuilder: WebClient.Builder,
    private val metricsService: MetricsService,
    private val exchangePriceService: ExchangePriceService
) : ExternalExchange {

    private val logger = org.slf4j.LoggerFactory.getLogger(WallexService::class.java)

    private val domain = "https://api.wallex.ir"

    // Configure WebClient with increased buffer size (5MB) to handle large responses
    private val webClient = webClientBuilder
        .baseUrl(domain)
        .exchangeStrategies(
            ExchangeStrategies.builder()
                .codecs { codecs: ClientCodecConfigurer ->
                    codecs.defaultCodecs().maxInMemorySize(5 * 1024 * 1024) // 5 MB
                }
                .build()
        )
        .build()

    override fun getExchangePrice(source: TokenEnum, destination: TokenEnum, precision: Int) {
        logger.info("Getting exchange price for $source to $destination")
        try {
            val response = getMarkets().block()
            response?.result?.markets?.let { markets ->
                // Wallex uses TMN (Toman) as quote currency, symbols are like USDTTMN, BTCTMN
                // We need to find the market where base_asset matches our token
                val symbol = "${destination}TMN"
                val market = markets.find { it.symbol?.equals(symbol, ignoreCase = true) == true }

                market?.let { marketData ->
                    val bidPrice = marketData.fairPrice?.bid ?: marketData.price
                    val askPrice = marketData.fairPrice?.ask ?: marketData.price
                    logger.info("Market data for $symbol: bidPrice=$bidPrice, askPrice=$askPrice, lastPrice=${marketData.price}")
                } ?: run {
                    logger.warn("Symbol $symbol not found in Wallex markets")
                }
            }
        } catch (e: Exception) {
            logger.error("Error fetching market data from Wallex", e)
        }
    }

    /**
     * Get all market data from Wallex
     * API: GET /hector/web/v1/markets
     */
    fun getMarkets(): Mono<WallexMarketResponse> {
        logger.debug("Fetching markets from Wallex")
        val startTime = System.nanoTime()

        return webClient.get()
            .uri("/hector/web/v1/markets")
            .retrieve()
            .bodyToMono(WallexMarketResponse::class.java)
            .doOnSuccess { response ->
                val duration = System.nanoTime() - startTime
                val durationMs = duration / 1_000_000
                metricsService.recordWallexResponseTime(duration)
                metricsService.recordWallexSuccess()
                logger.debug("Successfully fetched Wallex markets in ${durationMs}ms")

                // Save all market prices to database
                try {
                    response.result?.markets?.forEach { market ->
                        val symbol = market.symbol ?: return@forEach
                        val bidPrice = market.fairPrice?.bid?.toBigDecimalOrNull() ?: market.price?.toBigDecimalOrNull()
                        val askPrice = market.fairPrice?.ask?.toBigDecimalOrNull() ?: market.price?.toBigDecimalOrNull()
                        val lastPrice = market.price?.toBigDecimalOrNull()

                        // Extract asset from symbol (e.g., "BTCTMN" -> "BTC")
                        val asset = symbol.replace("TMN", "")

                        exchangePriceService.saveExchangePrice(
                            exchange = "Wallex",
                            symbol = symbol,
                            asset = asset,
                            quoteCurrency = "TMN",
                            bidPrice = bidPrice,
                            askPrice = askPrice,
                            lastPrice = lastPrice,
                            responseTimeMs = durationMs
                        )
                    }
                } catch (e: Exception) {
                    logger.error("Error saving Wallex prices to database", e)
                }
            }
            .doOnError { error ->
                val duration = System.nanoTime() - startTime
                metricsService.recordWallexResponseTime(duration)
                metricsService.recordWallexFailure()
                logger.error("Error calling Wallex markets API after ${duration / 1_000_000}ms", error)
            }
    }

    /**
     * Get specific market data for a symbol pair
     */
    fun getMarketForSymbol(source: TokenEnum, destination: TokenEnum): Mono<WallexMarketResponse> {
        return getMarkets()
    }
}
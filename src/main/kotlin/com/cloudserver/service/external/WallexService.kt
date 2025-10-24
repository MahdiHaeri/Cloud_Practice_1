package com.cloudserver.service.external

import com.cloudserver.dto.WallexMarketResponse
import com.cloudserver.enums.TokenEnum
import org.springframework.http.codec.ClientCodecConfigurer
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class WallexService(
    private val webClientBuilder: WebClient.Builder
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
            response?.result?.symbols?.let { symbols ->
                val symbol = "${source}${destination}".lowercase()
                symbols[symbol]?.let { marketData ->
                    logger.info("Market data for $symbol: bidPrice=${marketData.stats?.bidPrice}, askPrice=${marketData.stats?.askPrice}")
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
        return webClient.get()
            .uri("/hector/web/v1/markets")
            .retrieve()
            .bodyToMono(WallexMarketResponse::class.java)
            .doOnError { error ->
                logger.error("Error calling Wallex markets API", error)
            }
    }

    /**
     * Get specific market data for a symbol pair
     */
    fun getMarketForSymbol(source: TokenEnum, destination: TokenEnum): Mono<WallexMarketResponse> {
        return getMarkets()
    }
}
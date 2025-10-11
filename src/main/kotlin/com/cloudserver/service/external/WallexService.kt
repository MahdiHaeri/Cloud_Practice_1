package com.cloudserver.service.external

import com.cloudserver.enums.TokenEnum
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class WallexService(
    private val webClientBuilder: WebClient.Builder
) : ExternalExchange {

    private val logger = org.slf4j.LoggerFactory.getLogger(WallexService::class.java)

    private val domain = "https://api.wallex.com"
    private val webClient = webClientBuilder.baseUrl(domain).build()

    override fun getExchangePrice(source: TokenEnum, destination: TokenEnum, precision: Int) {
        logger.info("Getting exchange price for $source to $destination")
    }
}
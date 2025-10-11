package com.cloudserver.service.external

import com.cloudserver.enums.TokenEnum
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class NobitexService(
    private val webClientBuilder:  WebClient.Builder
) : ExternalExchange {

    private val logger = LoggerFactory.getLogger(NobitexService::class.java)

    private val domain = "https://api.nobitex.ir"
    private val webClient = webClientBuilder.baseUrl(domain).build()


    override fun getExchangePrice(source: TokenEnum, destination: TokenEnum, precision: Int) {
        logger.info("Getting exchange price for $source to $destination")
    }
}
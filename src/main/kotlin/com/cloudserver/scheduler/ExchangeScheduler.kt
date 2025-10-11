package com.cloudserver.scheduler

import com.cloudserver.enums.TokenEnum
import com.cloudserver.service.external.NobitexService
import com.cloudserver.service.external.WallexService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class ExchangeScheduler(
    private val nobitexService: NobitexService,
    private val wallexService: WallexService
) {

    @Scheduled(fixedDelay = 1000)
    fun getExchangePrice() {
        nobitexService.getExchangePrice(TokenEnum.USDT, TokenEnum.BTC, 8)
        wallexService.getExchangePrice(TokenEnum.USDT, TokenEnum.BTC, 8)
    }
}
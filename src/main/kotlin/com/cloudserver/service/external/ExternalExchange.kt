package com.cloudserver.service.external

import com.cloudserver.enums.TokenEnum

interface ExternalExchange {
    fun getExchangePrice(source: TokenEnum, destination: TokenEnum, precision: Int)
}
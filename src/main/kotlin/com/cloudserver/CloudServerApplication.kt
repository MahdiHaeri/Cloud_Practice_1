package com.cloudserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@SpringBootApplication
@EnableScheduling
class CloudServerApplication

fun main(args: Array<String>) {
    runApplication<CloudServerApplication>(*args)
}

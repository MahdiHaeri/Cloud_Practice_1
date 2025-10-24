package com.cloudserver.controller

import com.cloudserver.service.MetricsService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/metrics")
class MetricsController(
    private val metricsService: MetricsService
) {

    @GetMapping
    fun getMetricsSummary(): ResponseEntity<Map<String, Any>> {
        return ResponseEntity.ok(metricsService.getMetricsSummary())
    }
}


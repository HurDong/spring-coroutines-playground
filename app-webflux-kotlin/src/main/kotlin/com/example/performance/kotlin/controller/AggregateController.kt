package com.example.performance.kotlin.controller

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Timer
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.reactor.awaitSingle
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.reactive.function.client.WebClient
import java.util.concurrent.TimeUnit

@RestController
class AggregateController(
    private val webClientBuilder: WebClient.Builder,
    private val meterRegistry: MeterRegistry
) {
    private val webClient = webClientBuilder.baseUrl("http://localhost:8083").build()

    @GetMapping("/api/v1/aggregate")
    suspend fun aggregate(
        @RequestParam(defaultValue = "3") fanout: Int,
        @RequestParam(defaultValue = "200") delayMs: Long
    ): Map<String, Any> = coroutineScope {
        val start = System.currentTimeMillis()

        val deferreds = (1..fanout).map { id ->
            async { callDownstream(id, delayMs) }
        }

        val results = deferreds.map { it.await() }

        val totalLatency = System.currentTimeMillis() - start

        Timer.builder("aggregate.total.latency")
            .register(meterRegistry)
            .record(totalLatency, TimeUnit.MILLISECONDS)

        mapOf(
            "results" to results,
            "totalLatency" to totalLatency,
            "thread" to Thread.currentThread().name
        )
    }

    private suspend fun callDownstream(id: Int, delayMs: Long): Map<String, Any> {
        val start = System.currentTimeMillis()
        
        val result = webClient.get()
            .uri { it.path("/downstream/{id}").queryParam("delayMs", delayMs).build(id) }
            .retrieve()
            .bodyToMono(String::class.java)
            .awaitSingle()

        val latency = System.currentTimeMillis() - start

        Timer.builder("downstream.latency.tagged")
            .tag("id", id.toString())
            .register(meterRegistry)
            .record(latency, TimeUnit.MILLISECONDS)

        return mapOf(
            "id" to id,
            "result" to result,
            "latency" to latency,
            "thread" to Thread.currentThread().name
        )
    }
}

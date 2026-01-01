package com.example.performance.kotlin.controller

import kotlinx.coroutines.delay
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class DownstreamController {

    @GetMapping("/downstream/{id}")
    suspend fun downstream(@PathVariable id: String, @RequestParam(defaultValue = "200") delayMs: Long): String {
        delay(delayMs)
        return "response-$id"
    }
}

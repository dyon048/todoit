package com.example.todoit.data.remote.sheets

import kotlinx.coroutines.delay
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.random.Random

@Singleton
class RetryPolicy @Inject constructor() {

    /**
     * Executes [block] with exponential backoff + jitter.
     * Retries on HTTP 429 (quota), 500, 503.
     * On 401 the caller should re-authenticate — throws immediately.
     */
    suspend fun <T> executeWithRetry(
        maxRetries: Int = 5,
        block: suspend () -> T,
    ): T {
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: com.google.api.client.googleapis.json.GoogleJsonResponseException) {
                val code = e.statusCode
                if (code == 401) throw e // auth error — don't retry
                if (attempt >= maxRetries || code !in listOf(429, 500, 503)) throw e
                val backoffMs = min(60_000L, (1_000L shl attempt)) + Random.nextLong(1_000L)
                delay(backoffMs)
                attempt++
            } catch (e: IOException) {
                if (attempt >= maxRetries) throw e
                val backoffMs = min(30_000L, (500L shl attempt)) + Random.nextLong(500L)
                delay(backoffMs)
                attempt++
            }
        }
    }
}


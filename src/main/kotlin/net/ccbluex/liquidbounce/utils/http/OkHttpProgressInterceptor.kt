/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2025 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.utils.http

import okhttp3.Interceptor
import okhttp3.Response
import okio.Buffer
import okio.ForwardingSource
import okio.Source
import okio.buffer
import java.io.IOException

/**
 * OkHttp progress interceptor replacement for the MCEF version.
 * Tracks download progress for HTTP requests.
 */
class OkHttpProgressInterceptor(private val progressListener: ProgressListener) : Interceptor {

    fun interface ProgressListener {
        fun update(bytesRead: Long, contentLength: Long, done: Boolean)
    }

    @Throws(IOException::class)
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())
        val originalBody = originalResponse.body
        return if (originalBody != null) {
            originalResponse.newBuilder()
                .body(ProgressResponseBody(originalBody, progressListener))
                .build()
        } else {
            originalResponse
        }
    }

    private class ProgressResponseBody(
        private val responseBody: okhttp3.ResponseBody,
        private val progressListener: ProgressListener
    ) : okhttp3.ResponseBody() {

        private var bufferedSource: okio.BufferedSource? = null

        override fun contentType() = responseBody.contentType()

        override fun contentLength() = responseBody.contentLength()

        override fun source(): okio.BufferedSource {
            if (bufferedSource == null) {
                bufferedSource = source(responseBody.source()).buffer()
            }
            return bufferedSource!!
        }

        private fun source(source: Source): Source {
            return object : ForwardingSource(source) {
                var totalBytesRead = 0L

                @Throws(IOException::class)
                override fun read(sink: Buffer, byteCount: Long): Long {
                    val bytesRead = super.read(sink, byteCount)
                    totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                    progressListener.update(
                        totalBytesRead,
                        responseBody.contentLength(),
                        bytesRead == -1L
                    )
                    return bytesRead
                }
            }
        }
    }
}

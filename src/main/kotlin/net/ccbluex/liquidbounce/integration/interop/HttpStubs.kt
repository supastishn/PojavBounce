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

// Suppress unused parameter warnings for all stub implementations
@file:Suppress("UnusedParameter", "TooManyFunctions")

package net.ccbluex.liquidbounce.integration.interop

/**
 * Stub types to replace HTTP interop functionality since we're using native GUI instead of web-based UI.
 * These stubs prevent compilation errors while the HTTP server functionality is being phased out.
 */

// HTTP Method enum stub
enum class HttpMethod {
    GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS
}

// HTTP Request/Response stubs
data class RequestObject(
    val data: Any? = null,
    val queryParams: Map<String, String> = emptyMap(),
    val body: String = "",
    val method: HttpMethod = HttpMethod.GET,
    val params: Map<String, String> = emptyMap()
) {
    inline fun <reified T> asJson(): T = data as T
}

interface FullHttpResponse

// Create concrete implementation for return type compatibility
class StubHttpResponse(val data: Any) : FullHttpResponse

// HTTP Response functions stubs - return proper FullHttpResponse types
fun httpOk(data: Any): FullHttpResponse = StubHttpResponse(data)
fun httpBadRequest(message: String): FullHttpResponse = StubHttpResponse(message)
fun httpForbidden(message: String): FullHttpResponse = StubHttpResponse(message)
fun httpNotFound(message: String, description: String = ""): FullHttpResponse = StubHttpResponse(message)
fun httpInternalServerError(message: String): FullHttpResponse = StubHttpResponse(message)
fun httpNoContent(): FullHttpResponse = StubHttpResponse(Unit)
fun httpCreated(data: Any): FullHttpResponse = StubHttpResponse(data)
fun httpAccepted(data: Any): FullHttpResponse = StubHttpResponse(data)

// File streaming stub
fun httpFileStream(file: Any): FullHttpResponse = StubHttpResponse(file)
fun httpFile(file: Any): FullHttpResponse = StubHttpResponse(file)

// Image utilities stub
const val STUB_IMAGE_BASE64 = "data:image/png;base64," +
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8/5+hHgAHggJ/PchI7wAAAABJRU5ErkJggg=="

@Suppress("UnusedParameter")
fun readImageAsBase64(file: Any): String? = STUB_IMAGE_BASE64

// HTTP routing stubs
fun post(path: String, handler: (RequestObject) -> FullHttpResponse) { /* Stub - HTTP routing disabled */ }
fun get(path: String, handler: (RequestObject) -> FullHttpResponse) { /* Stub - HTTP routing disabled */ }
fun put(path: String, handler: (RequestObject) -> FullHttpResponse) { /* Stub - HTTP routing disabled */ }
fun delete(path: String, handler: (RequestObject) -> FullHttpResponse) { /* Stub - HTTP routing disabled */ }

// Node routing stubs for InteropFunctionRegistry
class Node {
    fun withPath(path: String, block: Node.() -> Unit): Node {
        // No-op for stub implementation
        return this
    }
    
    fun get(path: String, handler: (RequestObject) -> FullHttpResponse) { /* Stub - HTTP routing disabled */ }
    fun post(path: String, handler: (RequestObject) -> FullHttpResponse) { /* Stub - HTTP routing disabled */ }
    fun put(path: String, handler: (RequestObject) -> FullHttpResponse) { /* Stub - HTTP routing disabled */ }
    fun delete(path: String, handler: (RequestObject) -> FullHttpResponse) { /* Stub - HTTP routing disabled */ }
}

fun Node.withPath(path: String, block: Node.() -> Unit): Node = withPath(path, block)

// HTTP Server stubs
interface HttpServer {
    fun start()
    fun stop()
}

class HttpServerBuilder {
    fun port(port: Int) = this
    fun build(): HttpServer = object : HttpServer {
        override fun start() { /* Stub - HTTP server disabled */ }
        override fun stop() { /* Stub - HTTP server disabled */ }
    }
}

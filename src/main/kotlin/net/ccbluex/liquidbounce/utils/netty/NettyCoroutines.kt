package net.ccbluex.liquidbounce.utils.netty

import io.netty.bootstrap.ServerBootstrap
import io.netty.channel.Channel
import io.netty.channel.ChannelFuture
import io.netty.channel.EventLoopGroup
import io.netty.channel.epoll.Epoll
import io.netty.channel.epoll.EpollEventLoopGroup
import io.netty.channel.epoll.EpollServerSocketChannel
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.util.concurrent.Future
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

suspend fun <T> Future<T>.awaitSuspend(): T = suspendCancellableCoroutine { continuation ->
    addListener {
        if (it.isSuccess) {
            @Suppress("UNCHECKED_CAST")
            continuation.resume(it.get() as T)
        } else {
            continuation.resumeWithException(it.cause() ?: RuntimeException("Future failed"))
        }
    }
    continuation.invokeOnCancellation {
        cancel(true)
    }
}

suspend fun <T> Future<T>.syncSuspend(): T = awaitSuspend()

fun ServerBootstrap.setup(useNativeTransport: Boolean = false): Pair<EventLoopGroup, EventLoopGroup> {
    val bossGroup: EventLoopGroup
    val workerGroup: EventLoopGroup

    if (useNativeTransport && Epoll.isAvailable()) {
        bossGroup = EpollEventLoopGroup(1)
        workerGroup = EpollEventLoopGroup()
        this.group(bossGroup, workerGroup)
        this.channel(EpollServerSocketChannel::class.java)
    } else {
        bossGroup = NioEventLoopGroup(1)
        workerGroup = NioEventLoopGroup()
        this.group(bossGroup, workerGroup)
        this.channel(NioServerSocketChannel::class.java)
    }

    return Pair(bossGroup, workerGroup)
}

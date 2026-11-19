package live.citymall.okhttpnodelay

import java.net.InetAddress
import java.net.Socket
import javax.net.SocketFactory

/**
 * React Native flushes a request body before OkHttp writes the closing END_STREAM frame, so with
 * Nagle on, that last tiny write waits for an ACK: about one round trip added to every POST.
 */
object NoDelaySocketFactory : SocketFactory() {
    private val delegate: SocketFactory = SocketFactory.getDefault()

    override fun createSocket(): Socket = delegate.createSocket().noDelay()

    override fun createSocket(host: String, port: Int): Socket =
        delegate.createSocket(host, port).noDelay()

    override fun createSocket(
        host: String,
        port: Int,
        localHost: InetAddress,
        localPort: Int,
    ): Socket = delegate.createSocket(host, port, localHost, localPort).noDelay()

    override fun createSocket(host: InetAddress, port: Int): Socket =
        delegate.createSocket(host, port).noDelay()

    override fun createSocket(
        address: InetAddress,
        port: Int,
        localAddress: InetAddress,
        localPort: Int,
    ): Socket = delegate.createSocket(address, port, localAddress, localPort).noDelay()

    private fun Socket.noDelay(): Socket = apply { tcpNoDelay = true }
}

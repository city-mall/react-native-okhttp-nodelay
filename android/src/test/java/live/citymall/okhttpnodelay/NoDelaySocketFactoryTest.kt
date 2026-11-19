package live.citymall.okhttpnodelay

import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import kotlin.concurrent.thread
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NoDelaySocketFactoryTest {

    private val loopback: InetAddress = InetAddress.getByName("127.0.0.1")
    private lateinit var server: ServerSocket

    @Before
    fun listen() {
        server = ServerSocket(0, 50, loopback)
    }

    @After
    fun stopListening() {
        server.close()
    }

    private fun assertNoDelay(socket: Socket) = socket.use { assertTrue(it.tcpNoDelay) }

    @Test
    fun `unconnected socket, the one OkHttp asks for, has Nagle off`() =
        assertNoDelay(NoDelaySocketFactory.createSocket())

    @Test
    fun `host name overload has Nagle off`() =
        assertNoDelay(NoDelaySocketFactory.createSocket("127.0.0.1", server.localPort))

    @Test
    fun `host name overload with a local bind has Nagle off`() =
        assertNoDelay(NoDelaySocketFactory.createSocket("127.0.0.1", server.localPort, loopback, 0))

    @Test
    fun `address overload has Nagle off`() =
        assertNoDelay(NoDelaySocketFactory.createSocket(loopback, server.localPort))

    @Test
    fun `address overload with a local bind has Nagle off`() =
        assertNoDelay(NoDelaySocketFactory.createSocket(loopback, server.localPort, loopback, 0))

    @Test
    fun `a connection OkHttp opens through the factory has Nagle off`() {
        val serving = thread {
            server.accept().use { peer ->
                val request = peer.getInputStream().bufferedReader()
                while (!request.readLine().isNullOrEmpty()) Unit
                peer.getOutputStream().write(
                    "HTTP/1.1 200 OK\r\nContent-Length: 0\r\nConnection: close\r\n\r\n".toByteArray()
                )
            }
        }
        var noDelay: Boolean? = null
        val client = OkHttpClient.Builder()
            .socketFactory(NoDelaySocketFactory)
            .addNetworkInterceptor { chain ->
                noDelay = chain.connection()?.socket()?.tcpNoDelay
                chain.proceed(chain.request())
            }
            .build()

        client.newCall(Request.Builder().url("http://127.0.0.1:${server.localPort}/").build())
            .execute()
            .close()
        serving.join()

        assertEquals(true, noDelay)
    }
}

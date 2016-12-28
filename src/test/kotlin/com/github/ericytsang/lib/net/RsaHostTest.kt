package com.github.ericytsang.lib.net

import com.github.ericytsang.lib.net.host.RsaHost
import com.github.ericytsang.lib.net.host.TcpClient
import com.github.ericytsang.lib.net.connection.Connection
import com.github.ericytsang.lib.net.host.TcpServer
import org.junit.Test
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.InetAddress
import java.security.KeyPairGenerator
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

class RsaHostTest
{
    companion object
    {
        private const val TEST_PORT = 63294
    }

    @Test
    fun generalTest()
    {
        val tcpClient = TcpClient.anySrcPort()
        val tcpServer = TcpServer(TEST_PORT)

        // generate keypairs
        val serverKeypair = run()
        {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(512)
            keyGen.generateKeyPair()
        }
        val clientKeypair = run()
        {
            val keyGen = KeyPairGenerator.getInstance("RSA")
            keyGen.initialize(512)
            keyGen.generateKeyPair()
        }

        // establish connections
        val q = ArrayBlockingQueue<Connection>(1)
        thread {
            val rsaHost = RsaHost()
            q.put(rsaHost.connect(RsaHost.Address(
                {tcpServer.accept()},
                clientKeypair.public.encoded.toList(),
                serverKeypair.private.encoded.toList())))
        }
        val con1 = RsaHost()
            .connect(RsaHost.Address(
                {tcpClient.connect(TcpClient.Address(InetAddress.getLocalHost(),TEST_PORT))},
                serverKeypair.public.encoded.toList(),
                clientKeypair.private.encoded.toList()))
        val con2 = q.take()

        // exchange some data
        thread {
            con1.outputStream.let(::DataOutputStream).use {it.writeUTF("hello, friend!")}
        }
        println(con2.inputStream.let(::DataInputStream).use {it.readUTF()})
    }
}

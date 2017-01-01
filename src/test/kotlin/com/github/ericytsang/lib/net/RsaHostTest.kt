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
import java.util.concurrent.TimeoutException
import javax.security.sasl.AuthenticationException
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
                {TcpServer(TEST_PORT).use {it.accept()}},
                clientKeypair.public.encoded.toList(),
                serverKeypair.private.encoded.toList())))
        }
        val con1 = RsaHost()
            .connect(RsaHost.Address(
                {TcpClient.anySrcPort().connect(TcpClient.Address(InetAddress.getLocalHost(),TEST_PORT))},
                serverKeypair.public.encoded.toList(),
                clientKeypair.private.encoded.toList()))
        val con2 = q.take()

        // exchange some data
        thread {
            con2.inputStream.let(::DataInputStream).use {
                println(it.readUTF())
                println(it.readUTF())
                println(it.readUTF())
                println(it.readUTF())
                println(it.readUTF())
                println(it.readUTF())
                println(it.readUTF())
            }
        }
        con1.outputStream.let(::DataOutputStream).use {
            it.writeUTF("12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890")
            it.writeUTF("12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890")
            it.writeUTF("12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890")
            it.writeUTF("12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890")
            it.writeUTF("12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890")
            it.writeUTF("12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890")
            it.writeUTF("12345678901234567890123456789012345678901234567890123456789012345678901234567890123456789012345678901234567890")
        }
    }

    @Test
    fun badKeysTest()
    {
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
        val q = ArrayBlockingQueue<()->Unit>(1)
        thread {
            try
            {
                RsaHost().connect(RsaHost.Address(
                    {TcpServer(TEST_PORT).use {it.accept()}},
                    clientKeypair.public.encoded.toList(),
                    serverKeypair.private.encoded.toList()))
                q.put({throw RuntimeException("connection established")})
            }
            catch (ex:AuthenticationException) {
                q.put({Unit})
            }
        }
        try
        {
            RsaHost().connect(RsaHost.Address(
                {TcpClient.anySrcPort().connect(TcpClient.Address(InetAddress.getLocalHost(),TEST_PORT))},
                clientKeypair.public.encoded.toList(),
                clientKeypair.private.encoded.toList()))
            assert(false)
            {
                "connection established"
            }
        }
        catch (ex:AuthenticationException) {}
        q.take().invoke()
    }

    @Test
    fun timeoutTest()
    {
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
            TcpClient.anySrcPort().connect(TcpClient.Address(InetAddress.getLocalHost(),TEST_PORT))
                .let {q.put(it)}
        }
        try
        {
            RsaHost().connect(RsaHost.Address(
                {TcpServer(TEST_PORT).use {it.accept()}},
                clientKeypair.public.encoded.toList(),
                serverKeypair.private.encoded.toList()))
            assert(false)
        }
        catch (ex:TimeoutException) {}
        q.take()
    }

    @Test
    fun randomLongs()
    {
        for (i in 1..30)
        {
            println(randomBytes(8).toList())
        }
    }
}

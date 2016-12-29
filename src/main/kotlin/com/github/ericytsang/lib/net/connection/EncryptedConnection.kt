package com.github.ericytsang.lib.net.connection

import com.github.ericytsang.lib.cipherstream.CipherInputStream
import com.github.ericytsang.lib.cipherstream.CipherOutputStream
import com.github.ericytsang.lib.concurrent.future
import com.github.ericytsang.lib.concurrent.sleep
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.TimeoutException
import javax.crypto.Cipher
import javax.security.sasl.AuthenticationException
import kotlin.concurrent.thread

class EncryptedConnection(val underlyingConnection:Connection,val encodedEncryptingKey:ByteArray,val encodedDecryptingKey:ByteArray,val timeout:Long = EncryptedConnection.DEFAULT_AUTHENTICATION_TIMEOUT):Connection
{
    companion object
    {
        const val DEFAULT_AUTHENTICATION_TIMEOUT:Long = 5000
    }
    private val encryptingCipher:Cipher = run()
    {
        val cipher = Cipher.getInstance("RSA")
        val encryptingKey = KeyFactory.getInstance("RSA").generatePublic(X509EncodedKeySpec(encodedEncryptingKey))
        cipher.init(Cipher.ENCRYPT_MODE,encryptingKey)
        cipher
    }
    private val decryptingCipher:Cipher = run()
    {
        val cipher = Cipher.getInstance("RSA")
        val decryptingKey = KeyFactory.getInstance("RSA").generatePrivate(PKCS8EncodedKeySpec(encodedDecryptingKey))
        cipher.init(Cipher.DECRYPT_MODE,decryptingKey)
        cipher
    }
    override val inputStream:InputStream = CipherInputStream(underlyingConnection.inputStream,decryptingCipher)
    override val outputStream:OutputStream = CipherOutputStream(underlyingConnection.outputStream,encryptingCipher)
    override fun close()
    {
        inputStream.close()
        outputStream.close()
    }
    init
    {
        var isTimedOut = false
        val dataO = outputStream.let(::DataOutputStream)
        val dataI = inputStream.let(::DataInputStream)

        // set kill everything on timeout
        val killOnTimeout = thread {
            if (sleep(timeout).wasInterrupted) return@thread
            isTimedOut = true
            underlyingConnection.close()
        }

        // exchange some messages to make sure both parties have the appropriate
        // encoding and decoding ciphers
        try
        {
            val sentChallenge = Math.random()
            val f1 = future {
                dataO.writeDouble(sentChallenge)
                dataO.flush()
                Unit
            }
            val receivedChallenge = dataI.readDouble()
            f1.get()
            val f2 = future {
                dataO.writeDouble(receivedChallenge)
                dataO.flush()
                Unit
            }
            val receivedResponse = dataI.readDouble()
            f2.get()
            require(receivedResponse == sentChallenge)
            {
                "challenge was not responded to correctly"
            }
        }

        // if something goes wrong, clean up resources; do not close underlying
        // stream otherwise since it will be used throughout the lifetime of
        // this object
        catch (ex:Exception)
        {
            underlyingConnection.close()
            if (isTimedOut)
            {
                throw TimeoutException("authentication timed out")
            }
            else
            {
                throw AuthenticationException("authentication failed",ex)
            }
        }

        // kill the kill on timeout thread in any case
        finally
        {
            killOnTimeout.interrupt()
        }
    }
}

package com.github.ericytsang.lib.net.connection

import com.github.ericytsang.lib.cipherstream.CipherInputStream
import com.github.ericytsang.lib.cipherstream.CipherOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import kotlin.concurrent.thread

class EncryptedConnection(val underlyingConnection:Connection,val encodedEncryptingKey:ByteArray,val encodedDecryptingKey:ByteArray):Connection
{
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
        // exchange some messages to make sure both parties have the appropriate
        // encoding and decoding ciphers
        val dataO = outputStream.let(::DataOutputStream)
        val dataI = inputStream.let(::DataInputStream)
        val sentChallenge = Math.random()
        val t1 = thread {
            dataO.writeDouble(sentChallenge)
            dataO.flush()
        }
        val receivedChallenge = dataI.readDouble()
        t1.join()
        val t2 = thread {
            dataO.writeDouble(receivedChallenge)
            dataO.flush()
        }
        val receivedResponse = dataI.readDouble()
        t2.join()
        require(receivedResponse == sentChallenge)
        {
            "challenge was not responded to correctly"
        }
    }
}

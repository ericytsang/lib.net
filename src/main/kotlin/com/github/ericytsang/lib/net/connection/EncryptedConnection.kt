package com.github.ericytsang.lib.net.connection

import com.github.ericytsang.lib.cipherstream.CipherInputStream
import com.github.ericytsang.lib.cipherstream.CipherOutputStream
import com.github.ericytsang.lib.concurrent.future
import com.github.ericytsang.lib.concurrent.sleep
import com.github.ericytsang.lib.net.randomBytes
import com.github.ericytsang.lib.net.readByteArray
import java.io.InputStream
import java.io.OutputStream
import java.security.KeyFactory
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.concurrent.TimeoutException
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.security.sasl.AuthenticationException
import kotlin.concurrent.thread

class EncryptedConnection(val underlyingConnection:Connection,val encodedEncryptingKey:ByteArray,val encodedDecryptingKey:ByteArray,val timeout:Long = EncryptedConnection.DEFAULT_AUTHENTICATION_TIMEOUT):Connection
{
    companion object
    {
        const val AES_KEY_BYTE_LENGTH = 16
        const val AES_KEY_BLOCK_BYTE_LENGTH = 16
        const val CHALLENGE_BYTE_LENGTH = 8
        const val DEFAULT_AUTHENTICATION_TIMEOUT:Long = 5000
    }

    override val inputStream:InputStream

    override val outputStream:OutputStream

    override fun close()
    {
        inputStream.close()
        outputStream.close()
    }

    init
    {
        // prepare kill connection on timeout
        var isTimedOut = false
        val killOnTimeout = thread {
            if (sleep(timeout).wasInterrupted) return@thread
            isTimedOut = true
            underlyingConnection.close()
        }

        try
        {
            // set up encrypted & authenticated connection
            val encryptedStreams = encrypt(underlyingConnection.inputStream,underlyingConnection.outputStream)
            authenticate(encryptedStreams.first,encryptedStreams.second)
            inputStream = encryptedStreams.first
            outputStream = encryptedStreams.second

            // kill the kill on timeout thread because encryption and
            // authentication setup has completed before the kill on timeout
            // thread timed out...
            killOnTimeout.interrupt()
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
    }

    private fun authenticate(inputStream:InputStream,outputStream:OutputStream)
    {
        // set up rsa input and output streams
        val rsaO = run {
            val cipher = Cipher.getInstance("RSA")
            val encryptingKey = KeyFactory.getInstance("RSA")
                .generatePublic(X509EncodedKeySpec(encodedEncryptingKey))
            cipher.init(Cipher.ENCRYPT_MODE,encryptingKey)
            CipherOutputStream(outputStream,cipher)
        }
        val rsaI = run {
            val cipher = Cipher.getInstance("RSA")
            val decryptingKey = KeyFactory.getInstance("RSA")
                .generatePrivate(PKCS8EncodedKeySpec(encodedDecryptingKey))
            cipher.init(Cipher.DECRYPT_MODE,decryptingKey)
            CipherInputStream(inputStream,cipher)
        }

        // exchange some messages to make sure both parties have the
        // appropriate encoding and decoding ciphers
        run {
            val sentChallenge = randomBytes(CHALLENGE_BYTE_LENGTH)
            val f1 = future {
                rsaO.write(sentChallenge)
                rsaO.flush()
                Unit
            }
            val receivedChallenge = rsaI.readByteArray(CHALLENGE_BYTE_LENGTH)
            f1.get()
            val f2 = future {
                rsaO.write(receivedChallenge)
                rsaO.flush()
                Unit
            }
            val receivedResponse = rsaI.readByteArray(CHALLENGE_BYTE_LENGTH)
            f2.get()
            require(receivedResponse.toList() == sentChallenge.toList())
            {
                "challenge was not responded to correctly"
            }
        }
    }

    private fun encrypt(inputStream:InputStream,outputStream:OutputStream):Pair<InputStream,OutputStream>
    {
        // set up rsa input and output streams
        val rsaO = run {
            val cipher = Cipher.getInstance("RSA")
            val encryptingKey = KeyFactory.getInstance("RSA")
                .generatePublic(X509EncodedKeySpec(encodedEncryptingKey))
            cipher.init(Cipher.ENCRYPT_MODE,encryptingKey)
            CipherOutputStream(outputStream,cipher)
        }
        val rsaI = run {
            val cipher = Cipher.getInstance("RSA")
            val decryptingKey = KeyFactory.getInstance("RSA")
                .generatePrivate(PKCS8EncodedKeySpec(encodedDecryptingKey))
            cipher.init(Cipher.DECRYPT_MODE,decryptingKey)
            CipherInputStream(inputStream,cipher)
        }

        // generate AES key and IV to use for encrypting sent data
        val encryptingKey = SecretKeySpec(randomBytes(AES_KEY_BYTE_LENGTH),"AES")
        val encryptingIv = IvParameterSpec(randomBytes(AES_KEY_BLOCK_BYTE_LENGTH))
        val encryptingCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            .apply {init(Cipher.ENCRYPT_MODE,encryptingKey,encryptingIv)}
        val o = CipherOutputStream(outputStream,encryptingCipher)

        // send the AES key and IV to remote host
        val f1 = future {
            rsaO.write(encryptingKey.encoded)
            rsaO.write(encryptingIv.iv)
            rsaO.flush()
            Unit
        }

        // receive AES key and IV from remote host to use for decrypting
        // received data
        val decryptingKey = SecretKeySpec(rsaI.readByteArray(AES_KEY_BYTE_LENGTH),"AES")
        val decryptingIv = IvParameterSpec(rsaI.readByteArray(AES_KEY_BLOCK_BYTE_LENGTH))
        val decryptingCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            .apply {init(Cipher.DECRYPT_MODE,decryptingKey,decryptingIv)}
        val i = CipherInputStream(inputStream,decryptingCipher)
        f1.get()

        return i to o
    }
}

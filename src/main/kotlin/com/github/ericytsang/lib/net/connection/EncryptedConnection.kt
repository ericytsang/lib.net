package com.github.ericytsang.lib.net.connection

import com.github.ericytsang.lib.cipherstream.CipherInputStream
import com.github.ericytsang.lib.cipherstream.CipherOutputStream
import com.github.ericytsang.lib.concurrent.future
import com.github.ericytsang.lib.concurrent.sleep
import com.github.ericytsang.lib.net.randomBytes
import com.github.ericytsang.lib.net.readByteArray
import java.io.DataInputStream
import java.io.DataOutputStream
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
        // set kill everything on timeout
        var isTimedOut = false
        val killOnTimeout = thread {
            if (sleep(timeout).wasInterrupted) return@thread
            isTimedOut = true
            underlyingConnection.close()
        }

        try
        {
            val dataO = run {
                val cipher = Cipher.getInstance("RSA")
                val encryptingKey = KeyFactory.getInstance("RSA")
                    .generatePublic(X509EncodedKeySpec(encodedEncryptingKey))
                cipher.init(Cipher.ENCRYPT_MODE,encryptingKey)
                CipherOutputStream(underlyingConnection.outputStream,cipher)
                    .let(::DataOutputStream)
            }
            val dataI = run {
                val cipher = Cipher.getInstance("RSA")
                val decryptingKey = KeyFactory.getInstance("RSA")
                    .generatePrivate(PKCS8EncodedKeySpec(encodedDecryptingKey))
                cipher.init(Cipher.DECRYPT_MODE,decryptingKey)
                CipherInputStream(underlyingConnection.inputStream,cipher)
                    .let(::DataInputStream)
            }

            // exchange some messages to make sure both parties have the
            // appropriate encoding and decoding ciphers
            run {
                val sentChallenge = randomBytes(CHALLENGE_BYTE_LENGTH)
                val f1 = future {
                    dataO.write(sentChallenge)
                    dataO.flush()
                    Unit
                }
                val receivedChallenge = ByteArray(CHALLENGE_BYTE_LENGTH)
                dataI.readFully(receivedChallenge)
                f1.get()
                val f2 = future {
                    dataO.write(receivedChallenge)
                    dataO.flush()
                    Unit
                }
                val receivedResponse = ByteArray(CHALLENGE_BYTE_LENGTH)
                dataI.readFully(receivedResponse)
                f2.get()
                require(receivedResponse.toList() == sentChallenge.toList())
                {
                    "challenge was not responded to correctly"
                }
            }

            // generate AES key and IV to use for encrypting sent data
            val encryptingKey = SecretKeySpec(randomBytes(AES_KEY_BYTE_LENGTH),"AES")
            val encryptingIv = IvParameterSpec(randomBytes(AES_KEY_BLOCK_BYTE_LENGTH))
            val encryptingCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                .apply {init(Cipher.ENCRYPT_MODE,encryptingKey,encryptingIv)}
            outputStream = CipherOutputStream(underlyingConnection.outputStream,encryptingCipher)

            // send the AES key and IV to remote host
            val f1 = future {
                dataO.write(encryptingKey.encoded)
                dataO.write(encryptingIv.iv)
                dataO.flush()
                Unit
            }

            // receive AES key and IV from remote host to use for decrypting
            // received data
            val decryptingKey = SecretKeySpec(dataI.readByteArray(AES_KEY_BYTE_LENGTH),"AES")
            val decryptingIv = IvParameterSpec(dataI.readByteArray(AES_KEY_BLOCK_BYTE_LENGTH))
            val decryptingCipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
                .apply {init(Cipher.DECRYPT_MODE,decryptingKey,decryptingIv)}
            inputStream = CipherInputStream(underlyingConnection.inputStream,decryptingCipher)
            f1.get()
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

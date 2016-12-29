package com.github.ericytsang.lib.net.host

import com.github.ericytsang.lib.net.connection.Connection
import com.github.ericytsang.lib.net.connection.EncryptedConnection

class RsaHost:Client<RsaHost.Address>
{
    override fun connect(remoteAddress:Address):EncryptedConnection
    {
        return EncryptedConnection(
            remoteAddress.connectionFactory(),
            remoteAddress.encryptingKey.toByteArray(),
            remoteAddress.decryptingKey.toByteArray(),
            remoteAddress.authTimeoutMillis)
    }

    data class Address(
        val connectionFactory:()->Connection,
        val encryptingKey:List<Byte>,
        val decryptingKey:List<Byte>,
        val authTimeoutMillis:Long = EncryptedConnection.DEFAULT_AUTHENTICATION_TIMEOUT)
}

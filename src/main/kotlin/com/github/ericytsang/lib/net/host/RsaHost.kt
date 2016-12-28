package com.github.ericytsang.lib.net.host

import com.github.ericytsang.lib.net.connection.Connection
import com.github.ericytsang.lib.net.connection.EncryptedConnection

class RsaHost:Client<RsaHost.Address>
{
    override fun connect(remoteAddress:Address):Connection
    {
        // create the encrypted connection
        val underlyingConnection = remoteAddress.connectionFactory()
        val encryptingKey = remoteAddress.encryptingKey.toByteArray()
        val decryptingKey = remoteAddress.decryptingKey.toByteArray()
        return EncryptedConnection(underlyingConnection,encryptingKey,decryptingKey)
    }

    data class Address(
        val connectionFactory:()->Connection,
        val encryptingKey:List<Byte>,
        val decryptingKey:List<Byte>)
}

package com.github.ericytsang.lib.net.host

import com.github.ericytsang.lib.net.connection.Connection

interface Client<in Address>
{
    /**
     * connects to the remote host given the socket address and returns a
     * connection object.
     */
    fun connect(remoteAddress:Address):Connection
}

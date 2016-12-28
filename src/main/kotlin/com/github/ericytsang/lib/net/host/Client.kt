package com.github.ericytsang.lib.net.host

import com.github.ericytsang.lib.net.connection.Connection

/**
 * Created by etsang on 20/10/16.
 */

interface Client<in Address>
{
    /**
     * connects to the remote host given the socket address and returns a
     * connection object.
     */
    fun connect(remoteAddress:Address):Connection
}

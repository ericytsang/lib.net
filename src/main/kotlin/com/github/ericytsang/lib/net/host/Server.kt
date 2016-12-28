package com.github.ericytsang.lib.net.host

import com.github.ericytsang.lib.net.connection.Connection
import java.io.Closeable

interface Server:Closeable
{
    /**
     * accepts a connection from a remote host and returns a connection object.
     */
    fun accept():Connection
}

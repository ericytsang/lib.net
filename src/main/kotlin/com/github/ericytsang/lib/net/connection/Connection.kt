package com.github.ericytsang.lib.net.connection

import java.io.Closeable
import java.io.InputStream
import java.io.OutputStream

/**
 * represents an established duplex connection.
 */
interface Connection:Closeable
{
    /**
     * stream to read data from remote party.
     */
    val inputStream:InputStream

    /**
     * stream that sends data to the remote party.
     */
    val outputStream:OutputStream

    /**
     * closes the connection; both the input and output streams will also be
     * closed as a result.
     */
    override fun close()
}

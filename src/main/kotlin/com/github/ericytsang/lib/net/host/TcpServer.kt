package com.github.ericytsang.lib.net.host

import com.github.ericytsang.lib.net.connection.Connection
import com.github.ericytsang.lib.net.connection.TcpConnection
import java.net.ServerSocket

/**
 * Created by surpl on 10/28/2016.
 */
class TcpServer(listenPort:Int):Server
{
    private val serverSocket = ServerSocket(listenPort)

    override fun accept():Connection
    {
        return TcpConnection(serverSocket.accept())
    }

    override fun close()
    {
        serverSocket.close()
    }
}

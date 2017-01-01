package com.github.ericytsang.lib.net.host

import com.github.ericytsang.lib.net.connection.TcpConnection
import java.net.ServerSocket

class TcpServer(listenPort:Int):Server
{
    private val serverSocket = ServerSocket(listenPort)

    override fun accept():TcpConnection
    {
        return TcpConnection(serverSocket.accept())
    }

    override fun close()
    {
        serverSocket.close()
    }
}

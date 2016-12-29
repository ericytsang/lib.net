package com.github.ericytsang.lib.net.host

import com.github.ericytsang.lib.net.connection.TcpConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

class TcpClient private constructor(val socketFactory:()->Socket):Client<TcpClient.Address>
{
    companion object
    {
        fun anySrcPort():TcpClient
        {
            return TcpClient {Socket()}
        }

        fun srcPort(sourcePort:Int):TcpClient
        {
            return TcpClient()
            {
                val sock = Socket()
                sock.bind(InetSocketAddress(sourcePort))
                sock
            }
        }

        fun socketFactory(socketFactory:()->Socket):TcpClient
        {
            return TcpClient(socketFactory)
        }
    }

    override fun connect(remoteAddress:Address):TcpConnection
    {
        val socket = socketFactory()
        socket.connect(InetSocketAddress(remoteAddress.address,remoteAddress.port))
        return TcpConnection(socket)
    }

    data class Address(val address:InetAddress,val port:Int)
}

package com.github.ericytsang.lib.net.connection

import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.net.SocketException

class TcpConnection(val socket:Socket):Connection
{
    override val inputStream:InputStream = object:InputStream()
    {
        val stream = socket.inputStream
        override fun available():Int = stream.available()
        override fun close() = try
        {
            socket.shutdownInput()
        }
        catch (ex:SocketException)
        {
            Unit
        }
        override fun read():Int = try
        {
            stream.read()
        }
        catch (ex:SocketException)
        {
            -1
        }
        override fun read(b:ByteArray?):Int = try
        {
            stream.read(b)
        }
        catch (ex:SocketException)
        {
            -1
        }
        override fun read(b:ByteArray?,off:Int,len:Int):Int = try
        {
            stream.read(b,off,len)
        }
        catch (ex:SocketException)
        {
            -1
        }
    }
    override val outputStream:OutputStream = object:OutputStream()
    {
        val stream = socket.outputStream
        override fun write(b:Int) = stream.write(b)
        override fun write(b:ByteArray) = stream.write(b)
        override fun write(b:ByteArray?,off:Int,len:Int) = stream.write(b,off,len)
        override fun flush() = Unit
        override fun close() = try
        {
            socket.shutdownOutput()
        }
        catch (ex:SocketException)
        {
            Unit
        }
    }
    override fun close() = socket.close()
}

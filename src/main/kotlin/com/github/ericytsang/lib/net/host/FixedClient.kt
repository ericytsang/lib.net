package com.github.ericytsang.lib.net.host

import com.github.ericytsang.lib.net.connection.Connection

class FixedClient<Address>(val address:Address,val client:Client<Address>):Client<Unit>
{
    fun connect():Connection = client.connect(address)
    override fun connect(remoteAddress:Unit):Connection = connect()
}

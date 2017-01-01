package com.github.ericytsang.lib.net

import java.io.EOFException
import java.io.InputStream

private const val BITS_PER_BYTE = 8

fun randomBytes(numBytes:Int):ByteArray = (0..numBytes-1)
    .map {0..BITS_PER_BYTE-1}
    .map {
        bitOfAByte ->
        bitOfAByte
            .map {Math.random()}
            .map {Math.round(it).toInt()}
            .fold(0) {last,next -> last.shl(1).or(next)}
            .and(0xFF)
            .toByte()
    }
    .toByteArray()

fun InputStream.readByteArray(length:Int):ByteArray
{
    val b = ByteArray(length)
    var n = 0
    while (n < length)
    {
        val count = read(b,n,length-n)
        if (count < 0)
        {
            throw EOFException()
        }
        n += count
    }
    return b
}

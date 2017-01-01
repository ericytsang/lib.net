package com.github.ericytsang.lib.net

private const val BITS_PER_LONG = 64

fun randomLong() = (0..BITS_PER_LONG-1)
    .map {Math.random()}
    .map {Math.round(it)}
    .fold(0L) {last,next -> last.shl(1).or(next)}

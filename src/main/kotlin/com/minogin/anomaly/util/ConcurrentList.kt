package com.minogin.anomaly.util

import java.util.concurrent.locks.*
import kotlin.concurrent.*

class ConcurrentList<T> {
    private val lock = ReentrantLock()
    private val list = mutableListOf<T>()

    fun add(item: T) = lock.withLock { list.add(item) }

    fun remove(item: T) = lock.withLock { list.remove(item) }

    fun snapshot(): List<T> = lock.withLock { list.toList() }
}
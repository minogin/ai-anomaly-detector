package com.minogin.anomaly.internal.util

import java.util.concurrent.locks.*
import kotlin.concurrent.*

internal class ConcurrentList<T> {
    private val lock = ReentrantLock()
    private val list = mutableListOf<T>()

    fun add(item: T) = lock.withLock { list.add(item) }

    fun remove(item: T) = lock.withLock { list.remove(item) }

    fun updateLast(predicate: (T) -> Boolean, transform: (T) -> T): T? = lock.withLock {
        val idx = list.indexOfLast(predicate)
        if (idx >= 0) { list[idx] = transform(list[idx]); list[idx] } else null
    }

    fun snapshot(): List<T> = lock.withLock { list.toList() }
}
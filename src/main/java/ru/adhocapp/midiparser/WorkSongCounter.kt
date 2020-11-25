package ru.adhocapp.midiparser

import java.util.concurrent.atomic.AtomicLong

object WorkSongCounter {
    var value = AtomicLong()

    @JvmStatic
    fun get(): String {
        return value.getAndIncrement().toString().padStart(3, '0')
    }

}

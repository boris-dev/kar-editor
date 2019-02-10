package ru.adhocapp.midiparser

import java.io.File

internal class VbTrackTest {

    @org.junit.jupiter.api.Test
    fun getNotes() {
        val file = File("""C:\Users\Xiaomi\Desktop\vocaberry\google-drive-songs\ru\Ария\Герой Асфальта(2)_0.kar""")
        val midiFile = VbMidiFile("", "", file.inputStream())


        val vbTrack = VbTrack(0, midiFile.midiFile.tracks[1], TickInMs(0, 0, emptyList()))

        println(midiFile.midiFile.tracks[1].eventCount)

        println(vbTrack.notes.size)

        vbTrack.notes.forEach { println(it) }


        val tracks = VbMidiFile("", "", file.inputStream()).tracks
//        tracks.filter { it.name == "main" }.flatMap { it.notes }.forEach { println(it) }

        tracks.filter { it.name == "main" }.map { it.track }.forEach { println(it.size) }
        println(tracks.filter { it.name == "main" }.first().notes.size)

    }
}
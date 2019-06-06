package ru.adhocapp.midiparser

import ru.adhocapp.midiparser.domain.NoteSign
import ru.adhocapp.midiparser.domain.VbNote
import java.util.*

class TextNoteGraph(private val vocalNoteList: List<VbNote>) {

    fun text(): String {
        val allNotes = vocalNoteList.groupBy { it.note }.keys
        val sortedMap = vocalNoteList.groupBy { it.startTick }.toSortedMap()
        val noteTextLines = NoteTextLines(allNotes)
        for ((i, l) in sortedMap) {
            noteTextLines.add(l)
        }
        return noteTextLines.text()
    }

}

class NoteTextLines(allNotes: Set<NoteSign>) {
    private val noteLines: SortedMap<NoteSign, String>

    fun add(l: List<VbNote>) {

        val current = maxLength()

        for (vbNote in l) {
            val text = noteLines[vbNote.note]
            noteLines[vbNote.note] = text + " ".repeat(current - text!!.length) + vbNote.text
        }


    }

    private fun maxLength(): Int {
        return noteLines.map { it.value.length }.max() ?: 0
    }

    fun text(): String {
        return noteLines.map { it.value }.joinToString("\n")
    }


    init {
        val max = allNotes
                .maxBy { it.midi }!!
        val min = allNotes
                .minBy { it.midi }!!

        val range = NoteSign.range(min, max)

        noteLines = range.map { Pair(it, it.fullName() + "\t") }.toMap().toSortedMap()
    }

}

package ru.adhocapp.midiparser

import com.leff.midi.MidiTrack
import com.leff.midi.event.MidiEvent
import com.leff.midi.event.NoteOff
import com.leff.midi.event.NoteOn
import com.leff.midi.event.ProgramChange
import com.leff.midi.event.meta.Text
import com.leff.midi.event.meta.TrackName
import ru.adhocapp.midiparser.domain.NoteSign
import ru.adhocapp.midiparser.domain.VbNote
import java.lang.Exception
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.streams.toList


data class VbTrack(val id: Int, val track: MidiTrack, val tickInMs: TickInMs) {
    val name: String
    var notes: MutableList<VbNote>
    val instrumentName: String
    val sameNotesCount: Int
    val notesCount: Int
    val range: Int
    val highNote: String
    val lowNote: String
    var error: String? = ""

    var FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("mm:ss.SSS")

    init {
        notes = eventsToNotes(track.events).toMutableList()
        findErrors(notes)
        name = extractTrackName(track)
        instrumentName = extractInstrumentName(track)
        sameNotesCount = sameNotesCount(notes)
        notesCount = notes.size
        val extractHighNote = extractHighNote(notes)
        val extractLowNote = extractLowNote(notes)
        highNote = extractHighNote.fullName()
        lowNote = extractLowNote.fullName()
        range = Math.abs(extractHighNote.midi - extractLowNote.midi)
    }

    private fun findErrors(notes: List<VbNote>) {
        notes.forEach { it.error = if (containsExceptSelf(notes, it)) "error" else "" }
    }

    private fun eventsToNotes(events: TreeSet<MidiEvent>): List<VbNote> {
        val vbNotes = ArrayList<VbNote>()
        try {
            val noteOnQueue = LinkedBlockingQueue<NoteOn>()
            for (event in events) {
                if (event is NoteOn || event is NoteOff) {
                    if (isEndMarkerNote(event)) {
                        val noteSign = NoteSign.fromMidiNumber(extractNoteValue(event))
                        if (noteSign != NoteSign.UNDEFINED) {
                            val noteOn = noteOnQueue.poll()
                            vbNotes.add(
                                    VbNote(LocalDateTime.ofInstant(Instant.ofEpochMilli(tickInMs.msByTick(noteOn.tick)), ZoneId.systemDefault()).format(FORMATTER),
                                            noteSign,
                                            noteOn.tick,
                                            event.tick - noteOn.tick
                                    ))
                        }
                    } else {
                        noteOnQueue.offer(event as NoteOn)
                    }
                }
            }
        } catch (e: Exception) {
            error = e.message
        }

        return vbNotes
    }

    fun extractHighNote(list: List<VbNote>): NoteSign {
        return list
                .maxBy { it.note.midi }?.note ?: NoteSign.UNDEFINED

    }

    fun extractLowNote(list: List<VbNote>): NoteSign {
        return list
                .minBy { it.note.midi }?.note ?: NoteSign.UNDEFINED
    }


    private fun extractNoteValue(event: MidiEvent): Int? {
        return (event as? NoteOff)?.noteValue ?: if (event is NoteOn) {
            event.noteValue
        } else {
            null
        }
    }

    private fun isEndMarkerNote(event: MidiEvent): Boolean {
        return if (event is NoteOff) {
            true
        } else if (event is NoteOn) {
            event.velocity == 0
        } else {
            false
        }

    }

    private fun extractTrackName(midiTrack: MidiTrack): String {
        var name = midiTrack.events.stream()
                .filter { value -> value is TrackName }
                .map { midiEvent -> (midiEvent as TrackName).trackName }
                .findAny()
                .orElse("NO NAME")
        name = name.trim { it <= ' ' }
        name = name.replace("\"", "'")
        if (name.isEmpty()) {
            name = "NO NAME"
        }
        return name
    }

    private fun extractInstrumentName(midiTrack: MidiTrack): String {
        val instruments = midiTrack.events.stream()
                .filter { midiEvent -> midiEvent is ProgramChange }
                .map { midiEvent -> ProgramChange.MidiProgram.values()[(midiEvent as ProgramChange).programNumber].name }
                .toList()

        return when {
            instruments.size == 1 -> return instruments.first()
            instruments.size > 1 -> return instruments.toString()
            else -> "NO FOUND"
        }
    }

    private fun hasSameNotes(vbNotes: List<VbNote>): Boolean {
        return vbNotes.stream()
                .anyMatch { vbNote -> containsExceptSelf(vbNotes, vbNote) }
    }

    private fun sameNotesCount(vbNotes: List<VbNote>): Int {
        return Math.toIntExact(vbNotes.stream()
                .filter { vbNote -> containsExceptSelf(vbNotes, vbNote) }
                .count())
    }

    private fun containsExceptSelf(vbNotes: List<VbNote>, vbNote: VbNote): Boolean {
        return vbNotes.stream()
                .filter { vbNote1 -> vbNote1 != vbNote }
                .anyMatch { vbNote.intersect(it) }
    }

    fun hasText(): Boolean {
        return track.events.any { it is Text }
    }

    fun texts(): List<Text> {
        return track.events
                .filter { it is Text }
                .map { it as Text }
    }

    fun getTextAsOneString(): String {
        return notes.filter { it.note != NoteSign.UNDEFINED }.joinToString(separator = "\n") { it.text }
    }

    fun setTextForNotes(wholeText: String) {
        val filter = notes.filter { it.note != NoteSign.UNDEFINED }
        val split = wholeText.split("\n")
        for (i in 0 until split.size) {
            if (filter.size > i) {
                filter[i].text = split[i]
            }
        }

    }

    fun addMidiTextEvents(texts: List<Text>) {
        for (text in texts) {
            val find = notes.find { it.startTick == text.tick }
            if (find == null) {
                notes.add(
                        VbNote(LocalDateTime.ofInstant(Instant.ofEpochMilli(tickInMs.msByTick(text.tick)), ZoneId.systemDefault()).format(FORMATTER), text))
            } else {
                find.text = text.text
            }
        }
        notes = notes.sortedBy { it.startTick }.toMutableList()
    }

    fun textFromNotes(): List<String> {
        return notes.filter { it.note != NoteSign.UNDEFINED }.map { it.text }
    }


    fun stringWordFormat(): String {
        val textFromNotes = textFromNotes()
        val joinToString = textFromNotes.joinToString(separator = "") { it }
        return joinToString.replace("\\", "\n\n").replace("/", "\n")

//        val texts = textFromWordTrack()
//        val vbNotes = vocalNoteList()
//        return vbNotes
//                .map({ vbNote -> findText(texts, vbNote.startMs()) })
//                .toArray(String[]::new  /* Currently unsupported in Kotlin */)
    }

    private fun findText(texts: List<Text>, ms: Long?): String {

        for (i in texts.indices) {
            val text = texts[i]
            if (ms == tickInMs.msByTick(text.tick)) {
                return reformatText(
                        text.text.replace("[^\\S]+".toRegex(), " ").replace("[ ]+".toRegex(), " "),
                        if (i > 0) texts[i - 1].text else null,
                        if (i < texts.size - 1) texts[i + 1].text else null)
            }
        }

        return "-"
    }

    private fun reformatText(text: String, left: String?, right: String?): String {
        var result: String? = text
                .replace("\\\\".toRegex(), "")
                .replace("-".toRegex(), "")
                .replace("/".toRegex(), "")

        if (lastSymbolIsLetter(text) && firstSymbolIsLetter(right)) {
            result += "-"
        }

        if (firstSymbolIsLetter(text) && lastSymbolIsLetter(left)) {
            result = "-" + result!!
        }

        if (result == null || result.isEmpty()) {
            result = "-"
        }

        return result.trim { it <= ' ' }
    }

    private fun firstSymbolIsLetter(text: String?): Boolean {
        return if (text == null || text.isEmpty()) false else Character.isLetter(text[0])

    }

    private fun lastSymbolIsLetter(text: String?): Boolean {
        return if (text == null || text.isEmpty()) false else Character.isLetter(text[text.length - 1])
    }

}

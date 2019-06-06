package ru.adhocapp.midiparser

import com.leff.midi.MidiFile
import com.leff.midi.MidiTrack
import com.leff.midi.event.*
import com.leff.midi.event.meta.Tempo
import com.leff.midi.event.meta.Text
import com.leff.midi.event.meta.TrackName
import org.apache.commons.io.FileUtils
import ru.adhocapp.midiparser.domain.MlSongResultRow
import ru.adhocapp.midiparser.domain.NoteRange
import ru.adhocapp.midiparser.domain.NoteSign
import ru.adhocapp.midiparser.domain.VbNote
import ru.adhocapp.midiparser.utils.MidiTextToWordsOnNotes
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.LinkedBlockingQueue
import kotlin.streams.toList


/**
 * Created by Lenovo on 15.08.2017.
 */

class VbMidiFile(val fileName: String, val mimeType: String, inputStream: InputStream) {
    private var toneDifferenceFromOriginTone: Int = 0
    private var diffFromOriginTempoPercent: Float = 0.toFloat()
    val DRUM_INSTRUMENT_CODE_START_INDEX = 112
    val DRUM_INSTRUMENT_CHANNEL = 9
    var FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("mm:ss.SSS")

    lateinit var midiFile: MidiFile
    val tickInMs: TickInMs

    val vocalTrack: MidiTrack?
        get() {
            val words = getTrackByName("words")
            for (midiTrack in midiFile.tracks) {
                if (midiTrack !== words) {
                    if (isVocalTrack(midiTrack, words!!)) {
                        return midiTrack
                    }
                }
            }
            return null
        }

    var tracks: List<VbTrack>

    val text: String
        get() = wordTrackWithAnalyseManyTexts!!.events
                .stream()
                .filter { midiEvent -> midiEvent is Text }
                .map { midiEvent -> info(midiEvent as Text) + "\n" }
                .reduce { sum, next -> sum + next }
                .get()

    private val wordTrackWithAnalyseManyTexts: MidiTrack?
        get() {
            var words = getTrackByName("words")
            if (words == null) {
                words = findTrackWithManyTexts()
            }
            return words
        }


    init {
        try {
            inputStream.use { `is` ->
                this.midiFile = MidiFile(`is`)
                this.toneDifferenceFromOriginTone = 0
                this.diffFromOriginTempoPercent = 1f
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
        this.tickInMs = TickInMs(midiFile.resolution, midiFile.lengthInTicks, getAllTempos())
        this.tracks = midiFile.tracks.mapIndexed { index, midiTrack -> VbTrack(index, midiTrack, tickInMs) }
    }


    private fun base64ToMidiFile(base64EncodedString: String): MidiFile {
        try {
            ByteArrayInputStream(Base64.getDecoder().decode(cutStartTrashData(base64EncodedString).toByteArray()))
                    .use { `is` ->
                        return MidiFile(`is`)
                    }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    fun transponateLeaveInstrumentsInTheirOctaveIgnoreDrums(vocal: VbTrack, toneDiff: Int) {
        for (midiTrack in tracks) {
            val toneDiffValue = if (vocal == midiTrack) toneDiff else leaveInSameOctave(toneDiff)
            if (!isDrums(midiTrack.track)) {
                for (midiEvent in midiTrack.track.events) {
                    if (midiEvent is NoteOn) {
                        midiEvent.noteValue += toneDiffValue
                    } else if (midiEvent is NoteOff) {
                        midiEvent.noteValue += toneDiffValue
                    }
                }
            }
        }
        this.tracks = midiFile.tracks.mapIndexed { index, midiTrack -> VbTrack(index, midiTrack, tickInMs) }
    }


    private fun cutStartTrashData(base64EncodedString: String): String {
        var base64EncodedString = base64EncodedString
        if (base64EncodedString.startsWith("data:audio/mid;base64,")) {
            base64EncodedString = base64EncodedString.replace("data:audio/mid;base64,", "")
        }
        if (base64EncodedString.startsWith("data:;base64,")) {
            base64EncodedString = base64EncodedString.replace("data:;base64,", "")
        }
        return base64EncodedString
    }

    private fun isDrums(midiTrack: MidiTrack): Boolean {
        return (midiTrack.events).stream()
                .filter { midiEvent -> midiEvent is ProgramChange }
                .anyMatch { value -> (value as ProgramChange).programNumber >= DRUM_INSTRUMENT_CODE_START_INDEX }
                || midiTrack.events.filter { it is ChannelEvent }.all { (it as ChannelEvent).channel == DRUM_INSTRUMENT_CHANNEL }
    }

    private fun isVocalTrack(midiTrack: MidiTrack): Boolean {
        return (midiTrack.events).stream()
                .filter { value -> value is TrackName }
                .anyMatch { value -> (value as TrackName).trackName.toLowerCase() == "main" }
    }

    private fun isVocalTrack(midiTrack: MidiTrack, words: MidiTrack): Boolean {
        val textEvents = words.events.stream()
                .filter { midiEvent -> midiEvent is Text }
                .map { midiEvent -> midiEvent as Text }
                .toList()

        val failCount = textEvents.stream()
                .filter { text -> !containsTickInNoteOnEvent(midiTrack, text.tick) }
                .count()

        val rangeInSemitones = extractRange(midiTrack)

        if (rangeInSemitones <= 4) {
            return false
        }

        val l = failCount / textEvents.size.toFloat() * 100
        return if (l < 10) {
            try {
                val vbNotes = vocalNoteList(midiTrack) ?: return false

                if (hasSameNotes(vbNotes)) {
                    return false
                }

                Math.abs(textEvents.size - vbNotes.size) / textEvents.size.toFloat() * 100 < 5
            } catch (e: Throwable) {
                false
            }

        } else {
            false
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

    private fun containsTickInNoteOnEvent(midiTrack: MidiTrack, tick: Long): Boolean {
        return midiTrack.events.stream()
                .filter { midiEvent -> midiEvent is NoteOn && midiEvent.velocity > 0 }
                .anyMatch { midiEvent -> midiEvent.tick == tick }
    }

    private fun leaveInSameOctave(toneDiff: Int): Int {
        val i = toneDiff % 12
        return when {
            Math.abs(i) <= 6 -> i
            i > 0 -> i - 12
            else -> i + 12
        }

    }


    private fun generateOtherSamplesForTrackByTransponationScheme(midiTrack: MidiTrack, lengthInTicks: Long, transponationScheme: List<Int>) {
        val sampleEvents = copySampleEvents(midiTrack.events)
        generateSamplesByTransponationScheme(midiTrack, lengthInTicks, sampleEvents, transponationScheme)
    }


    private fun copySampleEvents(events: TreeSet<MidiEvent>): TreeSet<MidiEvent> {
        val midiEvents = TreeSet<MidiEvent>()
        midiEvents.addAll(events)
        return midiEvents
    }

    private fun generateSamplesByTransponationScheme(midiTrack: MidiTrack, lengthInTicks: Long, sampleEvents: TreeSet<MidiEvent>, transponationScheme: List<Int>) {
        var currentTick: Long? = 0L
        for (i in transponationScheme.indices) {
            val transponationDiff = transponationScheme[i]
            currentTick = lengthInTicks * (i + 1)
            addSample(midiTrack, currentTick, sampleEvents, transponationDiff)
        }
    }

    private fun addSample(midiTrack: MidiTrack, currentTick: Long?, sampleEvents: TreeSet<MidiEvent>, t: Int) {
        for (sampleEvent in sampleEvents) {
            if (sampleEvent is NoteOn) {
                midiTrack.insertEvent(createEvent(sampleEvent, currentTick, t))
            }

            if (sampleEvent is NoteOff) {
                midiTrack.insertEvent(createEvent(sampleEvent, currentTick, t))
            }
        }
    }

    private fun createEvent(noteOn: NoteOn, currentTick: Long?, transponation: Int): MidiEvent {
        return NoteOn(currentTick!! + noteOn.tick, noteOn.channel, noteOn.noteValue + transponation, noteOn.velocity)
    }

    private fun createEvent(noteOff: NoteOff, currentTick: Long?, transponation: Int): MidiEvent {
        return NoteOff(currentTick!! + noteOff.tick, noteOff.channel, noteOff.noteValue + transponation, noteOff.velocity)
    }


    fun durationTicks(): Long? {
        return midiFile!!.lengthInTicks
    }


    fun withTempo(value: Float): VbMidiFile {
        val result = MidiFile(midiFile!!.resolution)
        for (midiTrack in midiFile!!.tracks) {
            val resultTrack = MidiTrack()
            for (midiEvent in midiTrack.events) {
                if (midiEvent is Tempo) {
                    midiEvent.bpm = midiEvent.bpm * value / diffFromOriginTempoPercent
                    resultTrack.insertEvent(midiEvent)
                } else {
                    resultTrack.insertEvent(midiEvent)
                }
            }
            result.addTrack(resultTrack)
        }

        midiFile = result
        diffFromOriginTempoPercent = value
        return this
    }

    fun statByTrack(): List<String> {
        return midiFile!!.tracks.stream()
                .map { this.statOfTrack(it) }
                .toList()
    }

    private fun statOfTrack(midiTrack: MidiTrack): String {
        val trackName = midiTrack.events.stream()
                .filter { midiEvent -> midiEvent is TrackName }
                .map { midiEvent -> (midiEvent as TrackName).trackName }
                .findFirst()
                .orElse("Без TrackName")

        val instruments = midiTrack.events.stream()
                .filter { midiEvent -> midiEvent is ProgramChange }
                .map { midiEvent -> ProgramChange.MidiProgram.values()[(midiEvent as ProgramChange).programNumber].name }
                .toList()


        return """
$trackName: ${midiTrack.eventCount} | $instruments | channel: ${trackChannel(midiTrack)} | lowNote: ${extractLowNote(midiTrack)} highNote: ${extractHighNote(midiTrack)}"""
    }

    fun tempoStat(): String {
        return " tempo: " + midiFile!!.tracks.stream().flatMap { midiTrack -> midiTrack.events.stream() }
                .filter { midiEvent -> midiEvent is Tempo }
                .map { midiEvent -> " bpm: " + (midiEvent as Tempo).bpm.toString() + " t: " + midiEvent.getTick() }
                .toList()
                .toString()
    }

    private fun noteIntersects(midiTrack: MidiTrack): String {
        var before: MidiEvent? = null
        for (midiEvent in midiTrack.events) {
            if (before != null) {
                if (before is NoteOn && midiEvent is NoteOn) {
                    return "Нет"
                }
            }
            before = midiEvent
        }
        return "Да"
    }

    @Throws(IOException::class)
    fun writeToFile(file: File) {
        midiFile.writeToFile(file)
    }

    fun markVocalTrackOrThrowException(list: List<MlSongResultRow>) {
        val vocalTrack = vocalTrackByAnalysedData(list)

        var trackName: TrackName? = vocalTrack
                .events
                .stream()
                .filter { midiEvent -> midiEvent is TrackName }
                .map { midiEvent -> midiEvent as TrackName }
                .findFirst()
                .orElse(null)
        if (trackName == null) {
            trackName = TrackName(0, 0, "main")
            vocalTrack.insertEvent(trackName)
        } else {
            trackName.setName("main")
        }
        for (midiEvent in vocalTrack
                .events
                .stream()
                .filter { midiEvent -> midiEvent is ProgramChange }
                .toList()) {
            val programChange = midiEvent as ProgramChange
            programChange.programNumber = 0
        }

    }

    private fun vocalTrackByAnalysedData(list: List<MlSongResultRow>): MidiTrack {
        val chosen = chooseOne(list)
        return findTrackByChannel(chosen.channel)
    }

    fun chooseOne(list: List<MlSongResultRow>): MlSongResultRow {
        return MlSongRowAnalyser(list).best()
    }

    private fun findTrackByChannel(channel: Int): MidiTrack {
        val find = midiFile.tracks.find {
            it.events.filter { it is NoteOn }.map { it as NoteOn }.any { it.channel == channel }
        } ?: throw RuntimeException("NO TRACK FOUND BY CHANNEL: $channel")
        return find
    }


    private fun info(text: Text): String {
        return text.text
    }

    fun setText(strings: List<String>) {
        val midiEvents = getTrackByName("words")!!.events.stream()
                .filter { midiEvent -> midiEvent is Text }
                .map { midiEvent -> midiEvent as Text }
                .toList()
        for (i in midiEvents.indices) {
            midiEvents[i].text = strings[i]
        }
    }

    fun vocalNoteList(midiTrack: MidiTrack): List<VbNote> {
        try {
            val events = midiTrack.events
            return eventsToNotes(events)
        } catch (e: Throwable) {
            e.printStackTrace()
            return ArrayList()
        }

    }

    private fun eventsToNotes(events: TreeSet<MidiEvent>): List<VbNote> {
        val vbNotes = ArrayList<VbNote>()
        var i = 0

        val noteOnQueue = LinkedBlockingQueue<NoteOn>()
        for (event in events) {
            if (event is NoteOn || event is NoteOff) {
                if (isEndMarkerNote(event)) {
                    val noteSign = NoteSign.fromMidiNumber(extractNoteValue(event))
                    if (noteSign != NoteSign.UNDEFINED) {
                        val noteOn = noteOnQueue.poll()
                        vbNotes.add(
                                VbNote(LocalDateTime.ofInstant(Instant.ofEpochMilli(tickInMs.msByTick(noteOn.tick)), ZoneId.systemDefault()).format(FORMATTER), noteSign, noteOn.tick, event.tick - noteOn.tick))
                    }
                } else {
                    noteOnQueue.offer(event as NoteOn)
                }
            }
        }
        return vbNotes
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

    fun textFromWordTrack(): List<Text> {
        var words = getTrackByName("words")
        if (words == null) {
            words = midiFile!!.tracks[2]
        }

        return words!!.events.stream()
                .filter { value -> value is Text }
                .filter { value -> !(value as Text).text.startsWith("@") }
                .map { midiEvent -> midiEvent as Text }
                .toList()
    }

    fun getTrackByName(trackName: String): MidiTrack? {
        for (midiTrack in midiFile.tracks) {
            if (midiTrack.events.stream()
                            .filter { value -> value is TrackName }
                            .anyMatch { value -> (value as TrackName).trackName.toLowerCase() == trackName.toLowerCase() }) {
                return midiTrack
            }
        }
        return null
    }

    private fun hasNoteOffEvents(events: TreeSet<MidiEvent>): Boolean {
        return events.stream()
                .anyMatch { value -> value is NoteOff }
    }


    fun hasWords(): Boolean {
        return getTrackByName("words") != null
    }

    fun hasVocal(): Boolean {
        val words = getTrackByName("words")
        if (!isWordsCorrupted(words)) {
            for (midiTrack in midiFile.tracks) {
                if (midiTrack !== words) {
                    if (isVocalTrack(midiTrack, words!!)) {
                        return true
                    }
                }
            }
        }
        return false
    }

    private fun isWordsCorrupted(words: MidiTrack?): Boolean {
        return if (words == null) {
            true
        } else words.events.stream().filter { midiEvent -> midiEvent is Text }.count() < 10
    }

    fun differentTempoCount(): Long {
        val collect = midiFile!!.tracks.stream()
                .flatMap { midiTrack -> midiTrack.events.stream() }
                .filter { midiEvent -> midiEvent is Tempo }
                .map { midiEvent -> midiEvent as Tempo }
                .toList()

        val buff = HashSet<Long>()
        for (i in collect.indices) {
            buff.add(java.lang.Float.valueOf(collect[i].bpm).toLong())
        }

        return buff.size.toLong()
    }

    fun vocalChannel(): String {
        val vocalTrack = vocalTrack
        val collect = vocalTrack!!.events.stream()
                .filter { midiEvent -> midiEvent is NoteOn }
                .mapToInt { value -> (value as NoteOn).channel }
                .toList()
                .toSet()

        return collect.toString()
    }

    fun trackChannel(track: MidiTrack): Int? {
        return track.events
                .filter { midiEvent -> midiEvent is NoteOn }
                .map { value -> (value as NoteOn).getChannel() }
                .toList()
                .first()
    }

    fun addWordTrackByWords(lyrics: String) {
        val main = getTrackByName("main")
        val words = MidiTextToWordsOnNotes(lyrics).words()
        midiFile!!.addTrack(createWordTrack(main!!, words))

    }

    private fun createWordTrack(main: MidiTrack, words: Array<String>): MidiTrack {
        val result = MidiTrack()
        result.insertEvent(TrackName(0, 0, "words"))
        val collect = main.events.stream()
                .filter { midiEvent -> midiEvent is NoteOn }
                .filter { midiEvent -> (midiEvent as NoteOn).velocity > 0 }
                .toList()

        for (i in collect.indices) {
            val midiEvent = collect[i]
            result.insertEvent(Text(midiEvent.tick, 0, getWord(words, i)))
        }
        return result
    }

    private fun getWord(words: Array<String>, i: Int): String {
        return if (i < words.size) {
            words[i]
        } else ""
    }

    fun analyse(): String {
        var result = ""
        result += checkWordsExistsAnalyse()
        result += checkVocalPartExists()
        return result
    }

    private fun checkVocalPartExists(): String {
        return if (!hasVocal()) {
            "NO VOCAL, "
        } else ""
    }

    private fun checkWordsExistsAnalyse(): String {
        try {
            var words = getTrackByName("words")
            if (words == null) {
                words = findTrackWithManyTexts()
            }
            if (words == null) {
                return "NO WORDS, "
            }
        } catch (e: Exception) {
            return e.message + ", "
        }

        return ""
    }

    private fun findTrackWithManyTexts(): MidiTrack? {

        val collect = midiFile!!.tracks.stream()
                .filter { midiTrack -> checkTrackWithManyTexts(midiTrack) }
                .toList()

        if (collect.size > 1) {
            throw RuntimeException("MANY WORD TRACKS")
        }
        return if (collect.isEmpty()) {
            null
        } else {
            collect[0]
        }
    }

    private fun checkTrackWithManyTexts(midiTrack: MidiTrack): Boolean {
        val count = midiTrack.events.stream()
                .filter { midiEvent -> midiEvent is Text }
                .count()
        return count > 10
    }

    @Throws(IOException::class)
    fun analyseTracksData(output: File, filename: String) {
        val wordTrackWithAnalyseManyTexts = wordTrackWithAnalyseManyTexts

        for (midiTrack in midiFile!!.tracks) {
            val vbNotes = vocalNoteList(midiTrack)
            if (!vbNotes!!.isEmpty()) {
                val range = rangeFromTrack(vbNotes)
                FileUtils.write(output,
                        "\"" + filename + "\"," +
                                "\"" + extractTrackName(midiTrack) + "\"," +
                                "\"" + trackChannel(midiTrack) + "\"," +
                                "\"" + range.size() + "\"," +
                                "\"" + range.low().midi + "\"," +
                                "\"" + range.high().midi + "\"," +
                                "\"" + extractInstrument(midiTrack) + "\"," +
                                "\"" + extractNoteIntersectPercent(vbNotes) + "\"," +
                                "\"" + extractNoteTextCountDiffPercent(vbNotes, wordTrackWithAnalyseManyTexts!!) + "\"," +
                                "\"" + extractNoteTextTimeDiffPercent(midiTrack, wordTrackWithAnalyseManyTexts) + "\"," +
                                "\"" + (if (isVocalTrack(midiTrack, wordTrackWithAnalyseManyTexts)) "1" else "0") + "\"\n",
                        "UTF-8",
                        true)
            }
        }
    }

    @Throws(IOException::class)
    fun analyseTracksRawData(output: File, filename: String) {
        val wordTrackWithAnalyseManyTexts = wordTrackWithAnalyseManyTexts

        for (midiTrack in midiFile!!.tracks) {
            val vbNotes = vocalNoteList(midiTrack)
            if (!vbNotes!!.isEmpty()) {
                val range = rangeFromTrack(vbNotes)
                FileUtils.write(output,
                        "\"" + filename + "\"," +
                                "\"" + extractTrackName(midiTrack) + "\"," +
                                "\"" + trackChannel(midiTrack) + "\"," +
                                "\"" + range.size() + "\"," +
                                "\"" + range.low().midi + "\"," +
                                "\"" + range.high().midi + "\"," +
                                "\"" + extractInstrument(midiTrack) + "\"," +
                                "\"" + extractNoteIntersectPercent(vbNotes) + "\"," +
                                "\"" + extractNoteTextCountDiffPercent(vbNotes, wordTrackWithAnalyseManyTexts!!) + "\"," +
                                "\"" + extractNoteTextTimeDiffPercent(midiTrack, wordTrackWithAnalyseManyTexts) + "\"\n",
                        "UTF-8",
                        true)
            }
        }
    }

    private fun rangeFromTrack(vbNotes: List<VbNote>): NoteRange {
        if (vbNotes.isEmpty()) {
            return NoteRange(NoteSign.UNDEFINED, NoteSign.UNDEFINED)
        }

        val highestNote = vbNotes.stream()
                .max(Comparator.comparing<VbNote, Int> { o -> o.sign().midi })
                .get()

        val lowestNote = vbNotes.stream()
                .min(Comparator.comparing<VbNote, Int> { o -> o.sign().midi })
                .get()


        return NoteRange(lowestNote.sign(), highestNote.sign())

    }

    private fun extractNoteTextTimeDiffPercent(midiTrack: MidiTrack, textTrack: MidiTrack): String {
        val failCount = textTrack.events.stream()
                .filter { text -> !containsTickInNoteOnEvent(midiTrack, text.tick) }
                .count()

        val l = failCount / textTrack.eventCount.toFloat() * 100

        return l.toString()
    }

    private fun extractNoteTextCountDiffPercent(vbNotes: List<VbNote>, textTrack: MidiTrack): String {
        return (Math.abs(textTrack.eventCount - vbNotes.size) / Math.max(vbNotes.size, textTrack.eventCount).toFloat() * 100).toString()
    }

    private fun extractNoteIntersectPercent(vbNotes: List<VbNote>): String {
        return if (vbNotes.isEmpty()) {
            "-1"
        } else (sameNotesCount(vbNotes) / vbNotes.size.toFloat() * 100).toString()
    }

    private fun extractInstrument(midiTrack: MidiTrack): String {

        return midiTrack.events.stream()
                .filter { midiEvent -> midiEvent is ProgramChange }
                .filter { midiEvent -> ProgramChange.MidiProgram.values().size > (midiEvent as ProgramChange).programNumber }
                .map { midiEvent -> ProgramChange.MidiProgram.values()[(midiEvent as ProgramChange).programNumber].name }
                .findAny()
                .orElse("NO INSTRUMENT")
    }

    private fun extractHighNote(midiTrack: MidiTrack): String {
        val maxBy = midiTrack.events.filter { it is NoteOn }
                .map { it as NoteOn }
                .maxBy { it.noteValue }

        if (maxBy == null) {
            return "0"
        }

        return NoteSign.fromMidiNumber(maxBy.noteValue).fullName()
    }

    private fun extractLowNote(midiTrack: MidiTrack): String {
        val minBy = midiTrack.events.filter { it is NoteOn }
                .map { it as NoteOn }
                .minBy { it.noteValue }

        if (minBy == null) {
            return "0"
        }

        return NoteSign.fromMidiNumber(minBy.noteValue).fullName()
    }

    private fun extractRangeString(midiTrack: MidiTrack): String {
        return extractRange(midiTrack).toString()
    }

    private fun extractRange(midiTrack: MidiTrack): Int {
        val vbNotes = vocalNoteList(midiTrack)
        if (vbNotes!!.isEmpty()) {
            return 0
        }

        val highestNote = vbNotes.stream()
                .max(Comparator.comparing<VbNote, Int> { o -> o.sign().midi })
                .get()

        val lowestNote = vbNotes.stream()
                .min(Comparator.comparing<VbNote, Int> { o -> o.sign().midi })
                .get()


        return Math.abs(highestNote.sign().moreThanInSemitones(lowestNote.sign()))
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

    fun findWordTrackAndSetName() {
        var words = getTrackByName("words")
        if (words == null) {
            words = findTrackWithManyTexts()
            var trackName: TrackName? = words!!
                    .events
                    .stream()
                    .filter { midiEvent -> midiEvent is TrackName }
                    .map { midiEvent -> midiEvent as TrackName }
                    .findFirst()
                    .orElse(null)
            if (trackName == null) {
                trackName = TrackName(0, 0, "words")
                words.insertEvent(trackName)
            } else {
                trackName.setName("words")
            }
        }
    }

    fun getAllTempos(): List<Tempo> {
        return midiFile.tracks
                .flatMap { midiTrack -> midiTrack.events }
                .filter { value -> value is Tempo }
                .map { midiEvent -> midiEvent as Tempo }
                .toList()
    }

    fun inputStream(): InputStream {
        val tempFile = File.createTempFile(fileName, ".mid")
        tempFile.deleteOnExit()
        midiFile.writeToFile(tempFile)
        return tempFile.inputStream()
    }

    fun saveTextTrack(main: VbTrack) {
        val tracks: List<MidiTrack> = getAllTracksByName("main")
        tracks.forEach { setNameToTrack(it, "not-main") }
        setNameToTrack(main.track, "main")
        setGrandPianoToTrack(main.track)
        setMaxVolume(main.track)

        var words = getTrackByName("words")
        if (words != null) {
            words.events.clear()
        } else {
            words = MidiTrack()
            midiFile.addTrack(words)
        }
        val trackName = TrackName(0, 0, "words")
        words.insertEvent(trackName)
        main.notes.filter { it.note != NoteSign.UNDEFINED }.filter { it.text.isNotEmpty() }.forEach { words.insertEvent(Text(it.startTick, 0, it.text)) }

    }

    private fun setMaxVolume(track: MidiTrack) {
        track.events.stream()
                .filter { midiEvent -> midiEvent is NoteOn }
                .forEach { (it as NoteOn).velocity = 120 }
    }

    private fun setGrandPianoToTrack(track: MidiTrack) {
        track.events.stream()
                .filter { midiEvent -> midiEvent is ProgramChange }
                .forEach { (it as ProgramChange).programNumber = ProgramChange.MidiProgram.ACOUSTIC_GRAND_PIANO.programNumber() }
    }

    private fun setNameToTrack(midiTrack: MidiTrack, name: String) {
        var trackName: TrackName? = midiTrack
                .events
                .stream()
                .filter { midiEvent -> midiEvent is TrackName }
                .map { midiEvent -> midiEvent as TrackName }
                .findFirst()
                .orElse(null)
        if (trackName == null) {
            trackName = TrackName(0, 0, name)
            midiTrack.insertEvent(trackName)
        } else {
            trackName.setName(name)
        }

    }

    private fun getAllTracksByName(trackName: String): List<MidiTrack> {
        val midiTracks = mutableListOf<MidiTrack>()
        for (midiTrack in midiFile.tracks) {
            if (midiTrack.events.stream()
                            .filter { value -> value is TrackName }
                            .anyMatch { value -> (value as TrackName).trackName.toLowerCase() == trackName.toLowerCase() }) {
                midiTracks += midiTrack
            }
        }
        return midiTracks

    }

    fun getFbTrackByName(trackName: String): VbTrack? {
        return tracks.find { it.name == trackName }
    }


}


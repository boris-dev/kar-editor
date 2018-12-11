package ru.adhocapp.midiparser.domain

data class MlSongResultRow(var fileName: String, var trackName: String, var channel: Int, var range: Int, var lowNote: Int, var highNote: Int, var instrument: String, var noteIntersectPercent: Float, var noteTextCountDiffPercent: Float, var noteTextTimeDiffPercent: Float, var scoredLabels: Int, var scoredProbabilities: Float) {
    constructor(row: MutableMlSongResultRow) : this(row.fileName, row.trackName!!, row.channel!!, row.range!!, row.lowNote!!, row.highNote!!, row.instrument!!, row.noteIntersectPercent!!, row.noteTextCountDiffPercent!!, row.noteTextTimeDiffPercent!!, row.scoredLabels!!, row.scoredProbabilities!!)

    override fun toString(): String {
        return "MutableMlSongResultRow(fileName=$fileName, trackName=$trackName, channel=$channel, range=$range, lowNote=$lowNote, highNote=$highNote, instrument=$instrument, noteIntersectPercent=$noteIntersectPercent, noteTextCountDiffPercent=$noteTextCountDiffPercent, noteTextTimeDiffPercent=$noteTextTimeDiffPercent, scoredLabels=$scoredLabels, scoredProbabilities=$scoredProbabilities)"
    }


}

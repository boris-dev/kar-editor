package ru.adhocapp.midiparser.domain

data class MutableMlSongResultRow(var fileName: String = "", var trackName: String = "", var channel: Int = 0, var range: Int = 0, var lowNote: Int = 0, var highNote: Int = 0, var instrument: String = "", var noteIntersectPercent: Float = 0.0f, var noteTextCountDiffPercent: Float = 0.0f, var noteTextTimeDiffPercent: Float = 0.0f, var scoredLabels: Int = 0, var scoredProbabilities: Float = 0.0f) {


    override fun toString(): String {
        return "MutableMlSongResultRow(fileName=$fileName, trackName=$trackName, channel=$channel, range=$range, lowNote=$lowNote, highNote=$highNote, instrument=$instrument, noteIntersectPercent=$noteIntersectPercent, noteTextCountDiffPercent=$noteTextCountDiffPercent, noteTextTimeDiffPercent=$noteTextTimeDiffPercent, scoredLabels=$scoredLabels, scoredProbabilities=$scoredProbabilities)"
    }

    fun immutable(): MlSongResultRow {
        return MlSongResultRow(this)
    }

}

package ru.adhocapp.midiparser

import ru.adhocapp.midiparser.domain.MlSongResultRow

class MlSongRowAnalyser(val rows: List<MlSongResultRow>) {

//    fun chooseOne(): MutableMlSongResultRow {
//        val maxByscoredProbabilities = rows.maxBy { it.scoredProbabilities }
//        val minByNoteIntersectPercent = rows.minBy { it.noteIntersectPercent }
//        val minByNoteTextCountDiffPercent = rows.minBy { it.noteTextCountDiffPercent }
//        val minByNoteTextTimeDiffPercent = rows.minBy { it.noteTextTimeDiffPercent }
//
//    }

    fun hasBest(): Boolean {
        val maxByscoredProbabilities = rows.maxBy { it.scoredProbabilities }

        return maxByscoredProbabilities!!.noteTextTimeDiffPercent < 10

//        val minByNoteIntersectPercent = rows.minBy { it.noteIntersectPercent }
//        val minByNoteTextCountDiffPercent = rows.minBy { it.noteTextCountDiffPercent }
//        val minByNoteTextTimeDiffPercent = rows.minBy { it.noteTextTimeDiffPercent }
//        return maxByscoredProbabilities === minByNoteIntersectPercent
//                && maxByscoredProbabilities === minByNoteTextCountDiffPercent
//                && maxByscoredProbabilities === minByNoteTextTimeDiffPercent
    }

    fun bestString(): String {
        return rows.maxBy { it.scoredProbabilities }.toString()
    }

    fun best(): MlSongResultRow {
        return rows.maxBy { it.scoredProbabilities }!!
    }
}
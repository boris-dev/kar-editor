package ru.adhocapp.midiparser.domain

import com.leff.midi.event.NoteOn
import com.leff.midi.event.meta.Text


/**
 * Created by bshestakov on 13.07.2017.
 *
 *
 * Нота с длительностью
 */
data class VbNote(val time: String,
                  val note: NoteSign,
                  val startTick: Long,
                  val durationTicks: Long,
                  var text: String = "",
                  var error: String = ""
) {
    constructor(time: String, midiText: Text) : this(time, NoteSign.UNDEFINED, midiText.tick, 0L, midiText.text)

    fun sign(): NoteSign {
        return note
    }

    fun durationTicks(): Long {
        return durationTicks
    }

    fun startTick(): Long {
        return startTick
    }

    fun endTickInclusive(): Long {
        return startTick + durationTicks
    }

    fun equalsToNoteOn(noteOn: NoteOn): Boolean {
        return startTick() == noteOn.tick && sign().midi == noteOn.noteValue
    }

    fun isInAccuracyInterval(tick: Long): Boolean {
        return tick >= startTick - durationTicks && tick <= startTick + durationTicks * 2
    }


    override fun toString(): String {
        return "VbNote{" +
                "note=" + note +
                ", durationTicks=" + durationTicks +
                ", startTick=" + startTick +
                '}'.toString()
    }

    fun intersect(vbNote: VbNote): Boolean {
        return !(endTickInclusive() <= vbNote.startTick() || startTick >= vbNote.endTickInclusive())
    }

}

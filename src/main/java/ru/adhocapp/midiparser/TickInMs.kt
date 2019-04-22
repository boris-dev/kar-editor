package ru.adhocapp.midiparser

import com.leff.midi.event.MidiEvent
import com.leff.midi.event.meta.Tempo

import java.util.HashMap
import java.util.stream.Stream

/**
 * Created by Xiaomi on 11.09.2018.
 */

class TickInMs(private val resolution: Int, lengthInTicks: Long, tempoList: List<Tempo>) {
    private val tempoList: List<Tempo>
    private val tempoMsMap: MutableMap<Tempo, Int>

    init {
        val sortedTempo = tempoList.sortedBy { it.getTick() }.toList().toMutableList()
        if (sortedTempo.get(0).getTick() > 0) {
            sortedTempo.set(0, Tempo(0, sortedTempo.get(0).getDelta(), sortedTempo.get(0).getMpqn()))
        }
        this.tempoList = sortedTempo
        this.tempoMsMap = HashMap()
        var endTick = lengthInTicks
        for (i in sortedTempo.indices.reversed()) {
            val t = sortedTempo.get(i)
            tempoMsMap[t] = msInTick(t.getBpm(), endTick - t.getTick())
            endTick = t.getTick()
        }
    }

    fun msByTick(tick: Long): Long {
        if (tick == 0L) {
            return 0
        }
        val tempos = tempoList
                .filter { value -> value.tick < tick }
                .toList()
        var resultMs = 0
        if (tempos.size > 1) {
            resultMs = tempos
                    .dropLast(1)
                    .sumBy { tempoMsMap[it]!! }
        }
        val tempo = tempos[tempos.size - 1]
        resultMs += msInTick(tempo.bpm, tick - tempo.tick)
        return resultMs.toLong()
    }

    private fun msInTick(bpm: Float, amountOfTick: Long): Int {
        return (60 * 1000 / (bpm * resolution) * amountOfTick).toInt()
    }

    fun firstTempoBpm(): Int {
        return tempoList.first().bpm.toInt()
    }

}

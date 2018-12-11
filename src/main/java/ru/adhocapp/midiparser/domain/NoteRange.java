package ru.adhocapp.midiparser.domain;

import org.jetbrains.annotations.NotNull;

/**
 * Created by bshestakov on 06.09.2017.
 */

public class NoteRange {
    private final NoteSign lowestNote;
    private final NoteSign highestNote;


    public NoteRange(NoteSign lowestNote, NoteSign highestNote) {
        this.lowestNote = lowestNote;
        this.highestNote = highestNote;
    }

    public NoteSign high() {
        return highestNote;
    }

    public NoteSign low() {
        return lowestNote;
    }


    @Override
    public String toString() {
        return "NoteRange{" +
                "lowestNote=" + lowestNote.fullName() +
                ", highestNote=" + highestNote.fullName() +
                '}';
    }


    public int size() {
        return Math.abs(high().getMidi() - low().getMidi());
    }


    /**
     * Расстояние в полутонах диапазона до ноты, если больше нуля то нота выше диапазона если меньше то ниже
     * если 0 значит внутри диапазона
     *
     * @param noteSign нота для сравнения
     */
    public int distanceFrom(NoteSign noteSign) {
        Integer more = noteSign.moreThanInSemitones(high());
        if (more > 0) {
            return more;
        }
        Integer less = noteSign.moreThanInSemitones(low());
        if (less < 0) {
            return less;
        }
        return 0;
    }

    @NotNull
    public NoteRange transponate(int value) {
        return new NoteRange(lowestNote.someHigher(value), highestNote.someHigher(value));
    }
}

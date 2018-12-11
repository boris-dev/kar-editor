package ru.adhocapp.midiparser.utils;


import java.util.stream.Stream;

/**
 * Created by Xiaomi on 23.08.2018.
 */

public class MidiTextToWordsOnNotes {
    private final String midiText;

    public MidiTextToWordsOnNotes(String midiText) {
        this.midiText = midiText;
    }

    public String[] words() {
        return Stream.of(
                midiText
                        .replaceAll(" +|\n+", " ")
                        .replaceAll(" \\[", "[")
                        .replaceAll("\\[", "")
                        .replaceAll("=", "")
                        .replaceAll("] ", "]")
                        .replaceAll("]", " ")
                        .replaceAll("  ", " ")
                        .replaceAll("  ", " ")
                        .split(" "))
                .map(this::cutDashMakeSpace)
                .toArray(String[]::new);
    }

    private String cutDashMakeSpace(String s) {
        s = s.trim();
        if (s.startsWith("-")) {
            s = s.replace("-", "");
        }
        if (s.endsWith("-")) {
            s = s.substring(0, s.length() - 1);
        } else {
            s += " ";
        }
        return s;
    }

}

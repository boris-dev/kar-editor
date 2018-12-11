package ru.adhocapp.midiparser.domain;


import java.util.Arrays;


/**
 * Created by Lenovo on 18.09.2017.
 */

public enum HumanVoiceRange {
    SOPRANO(new NoteRange(NoteSign.C_4, NoteSign.C_6), "Сопрано"),
    MEZZO_SOPRANO(new NoteRange(NoteSign.A_3, NoteSign.A_5), "Меццо-сопрано"),
    CONTRALTO(new NoteRange(NoteSign.F_3, NoteSign.F_5), "Контральто"),
    COUNTERTENOR(new NoteRange(NoteSign.E_3, NoteSign.E_5), "Контртенор"),
    TENOR(new NoteRange(NoteSign.C_3, NoteSign.C_5), "Тенор"),
    BARITONE(new NoteRange(NoteSign.A_2, NoteSign.A_4), "Баритон"),
    BASS(new NoteRange(NoteSign.E_2, NoteSign.E_4), "Бас");

    private NoteRange vocalRange;
    private String stringRes;

    HumanVoiceRange(NoteRange vocalRange, String stringRes) {
        this.vocalRange = vocalRange;
        this.stringRes = stringRes;
    }

    public NoteRange getNoteRange() {
        return vocalRange;
    }


    public static HumanVoiceRange fromStringRes(String str) {
        return Arrays.asList(values()).stream()
                .filter(humanVoiceRange -> humanVoiceRange.stringRes.toLowerCase().equals(str.trim().toLowerCase()))
                .findAny()
                .orElseThrow(() -> new RuntimeException(str));
    }


}

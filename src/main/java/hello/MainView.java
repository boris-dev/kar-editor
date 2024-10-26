package hello;

import com.leff.midi.event.meta.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adhocapp.midiparser.TextNoteGraph;
import ru.adhocapp.midiparser.VbMidiFile;
import ru.adhocapp.midiparser.VbTrack;
import ru.adhocapp.midiparser.WorkSongCounter;
import ru.adhocapp.midiparser.domain.NoteSign;
import ru.adhocapp.midiparser.domain.VbNote;

import java.io.File;
import java.util.Iterator;
import java.util.List;

@Route
@StyleSheet("root.css")
public class MainView extends VerticalLayout {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);


    private final Grid<VbTrack> trackGrid;

    private final Grid<VbNote> noteGrid;

    private MemoryBuffer buffer;
    private VbMidiFile midiFile;
    private final Anchor downloadAnchor;
    private final TextArea textArea;

    private boolean isTransponated = false;

    public MainView() {
        this.trackGrid = new Grid<>(VbTrack.class);
        this.noteGrid = new Grid<>(VbNote.class);
        TextField transponateValue = new TextField();
        Button generateDownloadLink = new Button("Save", VaadinIcon.DOWNLOAD.create());
        Button transponate = new Button("Transponate", VaadinIcon.ARROWS_LONG_V.create());

        transponate.addClickListener(e -> {
            if (isTransponated) {
                Notification notification = new Notification(
                        "Only one transponation is posible for one upload", 3000);
                notification.open();
            } else {
                if (trackGrid.getSelectedItems().isEmpty()) {
                    Notification notification = new Notification(
                            "Need to choose track", 3000);
                    notification.open();
                } else {
                    midiFile.transponateLeaveInstrumentsInTheirOctaveIgnoreDrums(trackGrid.getSelectedItems().iterator().next(), Integer.parseInt(transponateValue.getValue()));
                    refreshContent();
                    isTransponated = true;
                }
            }
        });
        TextArea noteTextArea = new TextArea();
//        noteTextArea.setWidth("1000px");
//        noteTextArea.getElement().getChildren().findFirst().get();

        noteTextArea.getStyle().set("font-family", "Courier New");
        noteTextArea.getStyle().set("white-space", "nowrap");
//        noteTextArea.getStyle().set("white-space",  "pre-wrap");
//        noteTextArea.getStyle().set("overflow-y",  "auto ! important");
        Button text = new Button("Karaoke text", VaadinIcon.CLIPBOARD_TEXT.create());
        Button showNotes = new Button("Show notes", VaadinIcon.MUSIC.create());

        text.addClickListener(e -> {
            VbTrack track = trackGrid.getSelectedItems().iterator().next();
            TextArea textArea = new TextArea();
            textArea.setHeight("800px");
            textArea.setWidth("800px");
            textArea.setValue(track.stringWordFormat());
            Dialog dialog = new Dialog(textArea);
            dialog.setHeight("800px");
            dialog.open();
        });

        showNotes.addClickListener(e -> {
            if (midiFile != null) {
                VbTrack vbTrack = trackGrid.getSelectedItems().iterator().next();
                noteTextArea.setValue(new TextNoteGraph(vbTrack.getNotes()).text());//.replaceAll("\n", "<br>")
            }
        });

        this.buffer = new MemoryBuffer();

        Upload upload = new Upload(buffer);
        upload.addSucceededListener(event -> {
            logger.info("VbMidiFile refreshContent");
            midiFile = new VbMidiFile(buffer.getFileName(), buffer.getFileData().getMimeType(), buffer.getInputStream());
            refreshContent();
            isTransponated = false;
        });
        downloadAnchor = new Anchor();

        textArea = new TextArea();

//        textArea.setHeight("600px");
        textArea.addValueChangeListener(e -> {
            Iterator<VbTrack> iterator = trackGrid.getSelectedItems().iterator();
            if (iterator.hasNext()) {
                VbTrack track = iterator.next();
                if (!e.getValue().isEmpty()) {
                    track.setTextForNotes(e.getValue());
                }
                noteGrid.setItems(track.getNotes());
            }
        });

        HorizontalLayout actions = new HorizontalLayout(upload, generateDownloadLink, transponateValue, transponate, text, showNotes);
        SplitLayout layout1 = new SplitLayout(
                trackGrid,
                noteGrid);
        trackGrid.setHeight("600px");
        noteGrid.setHeight("600px");
        layout1.setSplitterPosition(33);
        SplitLayout layout2 = new SplitLayout(
                layout1, textArea
        );
        layout2.setWidth(getWidth());
        layout2.setSplitterPosition(90);


        SplitLayout layout = new SplitLayout(layout2, noteTextArea);
        layout.setOrientation(SplitLayout.Orientation.VERTICAL);
        layout.setWidth(getWidth());
        layout.setHeight(("700px"));
        layout.setSplitterPosition(80);

        add(actions, downloadAnchor, layout);

        initTrackGrid();

        transponateValue.setPlaceholder("0");
        transponateValue.setValue("0");
        transponateValue.setValueChangeMode(ValueChangeMode.EAGER);

        initNoteGrid();


        generateDownloadLink.addClickListener(e -> {
            try {
                midiFile.saveTextTrack(trackGrid.getSelectedItems().iterator().next());

                String fileName = midiFile.getFileName();
                fileName = FilenameUtils.removeExtension(fileName);
                fileName += ".kar";
                fileName = WorkSongCounter.get() + "_" + fileName;
                String finalFileName = fileName;
                StreamResource resource = new StreamResource(fileName,
                        () -> midiFile.inputStream(finalFileName));

                resource.setCacheTime(-1);
                resource.setContentType(midiFile.getMimeType());

                downloadAnchor.setText(fileName);
                downloadAnchor.setHref(resource);
            } catch (Exception ex) {
                Notification notification = new Notification(
                        "Error: " + ex.getMessage(), 3000);
                notification.open();
            }
        });
        trackGrid.asSingleSelect().addValueChangeListener(e -> {
            if (e != null && e.getValue() != null) {
                logger.info("GRIDSELECT: " + e.getValue().getName());
                noteGrid.setItems(e.getValue().getNotes());
            }
        });

    }

    private File createTempFolder() {
        File recent = new File("recent");
        recent.mkdirs();
        recent.mkdir();
        return recent;
    }

    private void initTrackGrid() {
        trackGrid.setColumns("id", "name");
        trackGrid.getColumnByKey("id").setWidth("40px").setFlexGrow(0);
        trackGrid.addColumn(new ComponentRenderer<>(track -> {
            if (track.hasText()) {
                Button button = new Button(VaadinIcon.CLIPBOARD_TEXT.create());
                button.addClickListener(e -> {
                    VbTrack next = trackGrid.getSelectedItems().iterator().next();
                    logger.info("ADDTEXT: " + next.getName());
                    List<Text> texts = track.texts();
                    next.addMidiTextEvents(texts);
                    refreshContent();
                    fillTextArea(next);
                    trackGrid.select(next);
                });
                return button;
            }
            return new Div();
        }));
        trackGrid.addColumn("instrumentName");
        trackGrid.addColumn("notesCount");
        trackGrid.addColumn("sameNotesCount");
        trackGrid.addColumn("lowNote");
        trackGrid.addColumn("highNote");
        trackGrid.addColumn("range");
        trackGrid.addColumn("tempoBpm");
        trackGrid.addColumn("error");

        trackGrid.getColumnByKey("name").setWidth("100px").setFlexGrow(0);
        trackGrid.getColumnByKey("instrumentName").setFlexGrow(1);
        trackGrid.getColumnByKey("notesCount").setFlexGrow(0);
        trackGrid.getColumnByKey("sameNotesCount").setFlexGrow(0);
        trackGrid.getColumnByKey("lowNote").setFlexGrow(0);
        trackGrid.getColumnByKey("highNote").setFlexGrow(0);
        trackGrid.getColumnByKey("range").setFlexGrow(0);
    }

    private void fillTextArea(VbTrack t) {
        textArea.setValue(t.getTextAsOneString());
    }

    private void initNoteGrid() {
        noteGrid.setColumns("time", "startTick", "durationTicks");

        noteGrid.getColumnByKey("time").setFlexGrow(0);
        noteGrid.getColumnByKey("startTick").setFlexGrow(0);
        noteGrid.getColumnByKey("durationTicks").setFlexGrow(0);

        noteGrid.addColumn(new ComponentRenderer<>(Div::new,
                (div, person) -> div.setText(person.getNote().fullName())))
                .setHeader("Note")
                .setFlexGrow(0);

        noteGrid.addColumn(new ComponentRenderer<>(note -> {
            TextField name = new TextField();
            name.setValue(note.getText());
            name.addValueChangeListener(event -> {
                note.setText(event.getValue());
                fillTextArea(trackGrid.getSelectedItems().iterator().next());
            });
            return name;
        })).setHeader("Text");

        noteGrid.addColumn(new ComponentRenderer<>(note -> {
            if (note.getNote() == NoteSign.UNDEFINED) {
                Button button = new Button(VaadinIcon.TRASH.create());
                button.addClickListener(e -> {
                    VbTrack next = trackGrid.getSelectedItems().iterator().next();
                    next.getNotes().remove(note);
                    noteGrid.setItems(next.getNotes());
                });
                return button;
            } else {
                return new Div();
            }
        }));

        noteGrid.addColumn(new ComponentRenderer<>(note -> {
            if (!note.getError().isEmpty()) {
                return new Icon(VaadinIcon.WARNING);
            }
            return new Div();

        })).setHeader("Error").setFlexGrow(0);
        noteGrid.setColumnReorderingAllowed(true);
    }

    private void refreshContent() {
        if (midiFile != null) {
            trackGrid.setItems(midiFile.getTracks());
            trackGrid.getDataProvider().refreshAll();
            if (trackGrid.getSelectedItems().isEmpty()) {
                VbTrack track = midiFile.getFbTrackByName("main");
                if (track != null) {
                    trackGrid.select(track);
                } else {
                    noteGrid.setItems();
                }
            } else {
                trackGrid.select(trackGrid.getSelectedItems().iterator().next());
            }
            noteGrid.getDataProvider().refreshAll();
            textArea.setValue("");
        }
    }

}

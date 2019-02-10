package hello;

import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.adhocapp.json.JsonWithLocalLinksAndFilesInZip;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

@Route("json-extractor")
public class JsonExtractorView extends VerticalLayout {
    private static final Logger logger = LoggerFactory.getLogger(Application.class);


    private MemoryBuffer buffer;
    private final Anchor downloadAnchor;
    private File currentFile;

    public JsonExtractorView() {
        this.buffer = new MemoryBuffer();

        Upload upload = new Upload(buffer);
        upload.addSucceededListener(event -> {
            logger.info("VbMidiFile refreshContent");
            if (currentFile != null) {
                currentFile.delete();
            }
            currentFile = new JsonWithLocalLinksAndFilesInZip(buffer.getFileName(), buffer.getInputStream()).zipFile();
            generateDownloadLink();
        });
        downloadAnchor = new Anchor();

        HorizontalLayout actions = new HorizontalLayout(upload);
        add(actions, downloadAnchor);
    }

    private void generateDownloadLink() {
        try {
            String fileName = currentFile.getName();
            fileName = FilenameUtils.removeExtension(fileName);
            fileName += ".zip";
            StreamResource resource = new StreamResource(fileName,
                    () -> {
                        try {
                            return new FileInputStream(currentFile);
                        } catch (FileNotFoundException e1) {
                            return null;
                        }
                    });
            resource.setCacheTime(-1);
            resource.setContentType("application/gzip");

            downloadAnchor.setText(fileName);
            downloadAnchor.setHref(resource);
        } catch (Exception ex) {
            Notification notification = new Notification(
                    "Error: " + ex.getMessage(), 3000);
            notification.open();
        }
    }


}

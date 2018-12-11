package hello;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;


public class PanelWithScrollableTextField extends VerticalLayout {
    public TextArea textArea;
    public PanelWithScrollableTextField() {
        textArea = new TextArea();
        textArea.setSizeFull();
        add(textArea);
        setSizeFull();
    }
}

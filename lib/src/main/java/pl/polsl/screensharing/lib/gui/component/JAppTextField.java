package pl.polsl.screensharing.lib.gui.component;

import pl.polsl.screensharing.lib.gui.input.SimpleDocumentFilter;

import javax.swing.*;
import javax.swing.text.AbstractDocument;

public class JAppTextField extends JTextField {
    private final int maxCharacters;
    private final String regex;

    public JAppTextField(int columns, int maxCharacters, String regex) {
        super(columns);
        this.maxCharacters = maxCharacters;
        this.regex = regex;
        setDocumentValidator();
    }

    private void setDocumentValidator() {
        ((AbstractDocument) getDocument()).setDocumentFilter(new SimpleDocumentFilter(maxCharacters, regex));
    }
}
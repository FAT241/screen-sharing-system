package pl.polsl.screensharing.lib.gui;

import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.awt.*;

public abstract class AbstractTextInfoPanel extends JPanel {
    private final JLabel textLabel;

    protected AbstractTextInfoPanel(String textContent, boolean isVisible) {
        setLayout(new BorderLayout());
        setVisible(isVisible);
        setBackground(UiTheme.SURFACE_LIGHT);

        textLabel = new JLabel(textContent);
        textLabel.setHorizontalAlignment(SwingConstants.CENTER);
        textLabel.setForeground(UiTheme.TEXT_SECONDARY);

        add(textLabel);
    }

    protected AbstractTextInfoPanel(String textContent) {
        this(textContent, false);
    }

    protected AbstractTextInfoPanel() {
        this(StringUtils.EMPTY, false);
    }

    protected void setTextLabel(String textContent) {
        textLabel.setText(textContent);
    }
}
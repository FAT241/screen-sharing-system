package pl.polsl.screensharing.lib.gui.component;

import pl.polsl.screensharing.lib.file.FileUtils;
import pl.polsl.screensharing.lib.gui.UiTheme;
import pl.polsl.screensharing.lib.icon.AppIcon;

import javax.swing.*;
import java.awt.*;

public class JAppIconButton extends JButton {
    public JAppIconButton(String text, AppIcon iconName) {
        this(text, iconName, false);
    }

    public JAppIconButton(String text, AppIcon iconName, boolean setDescription) {
        if (setDescription) {
            setToolTipText(text);
        }
        setText(text);
        FileUtils.getImageIconFromResources(getClass(), iconName).ifPresent(this::setIcon);

        setFocusPainted(false);
        setRolloverEnabled(true);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setIconTextGap(8);
        setHorizontalTextPosition(SwingConstants.RIGHT);
        setVerticalTextPosition(SwingConstants.CENTER);
    }

    public JAppIconButton(String text, AppIcon iconName, boolean setDescription, boolean isEnabled) {
        this(text, iconName, setDescription);
        setEnabled(isEnabled);
    }

    public void setAsPrimaryButton() {
        putClientProperty("FlatLaf.style", String.format("background: %s; foreground: #FFFFFF", UiTheme.hex(UiTheme.ACCENT)));
    }
}
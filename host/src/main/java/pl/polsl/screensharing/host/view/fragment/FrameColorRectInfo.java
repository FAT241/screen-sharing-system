package pl.polsl.screensharing.host.view.fragment;

import pl.polsl.screensharing.host.state.HostState;
import pl.polsl.screensharing.lib.gui.UiTheme;

import javax.swing.*;
import java.awt.*;

public class FrameColorRectInfo extends JPanel {
    private Color selectedColor;

    public FrameColorRectInfo(HostState hostState) {
        selectedColor = UiTheme.TEXT_SECONDARY;
        hostState.wrapAsDisposable(hostState.getFrameColor$(), color -> {
            selectedColor = color;
            repaint();
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.setColor(selectedColor);
        g.fillRect(0, 0, 30, 30);
        g.setColor(UiTheme.BORDER);
        g.drawRect(0, 0, 30, 30);
    }
}

package pl.polsl.screensharing.lib.gui;

import pl.polsl.screensharing.lib.AppType;
import pl.polsl.screensharing.lib.state.AbstractDisposableProvider;

import javax.swing.*;
import java.awt.*;
import java.util.Optional;

public abstract class AbstractRootFrame extends JFrame {
    private final AppType appType;
    private final AbstractDisposableProvider disposableProvider;
    private final Dimension size;
    private final JPanel rootPanel;
    protected final Optional<Image> imageIconOptional;

    protected AbstractRootFrame(
        AppType appType,
        AbstractDisposableProvider disposableProvider,
        Class<?> frameClazz
    ) {
        rootPanel = new JPanel();
        this.disposableProvider = disposableProvider;
        this.appType = appType;
        imageIconOptional = appType.getIconPath(frameClazz);
        size = appType.getRootWindowSize();
    }

    public void guiInitAndShow() {
        rootPanel.setBounds(0, 0, getWidth(), getHeight());
        imageIconOptional.ifPresent(this::setIconImage);

        setSize(size);
        setMinimumSize(UiScale.scale(new Dimension(960, 540)));
        setLocation(getMotherScreenCenter());
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new GuiWindowAdapter(this, disposableProvider));
        setTitle(appType.getRootWindowTitle());
        setLayout(new BorderLayout());
        add(rootPanel, BorderLayout.CENTER);
        extendsFrame(this, rootPanel);
        setVisible(true);
    }

    private Point getMotherScreenCenter() {
        final Rectangle bounds = getGraphicsConfiguration().getBounds();
        final int x = bounds.x + bounds.width / 2 - getWidth() / 2;
        final int y = bounds.y + bounds.height / 2 - getHeight() / 2;
        return new Point(Math.max(x, bounds.x), Math.max(y, bounds.y));
    }

    protected abstract void extendsFrame(JFrame frame, JPanel rootPanel);
}

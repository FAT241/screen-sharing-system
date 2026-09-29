package pl.polsl.screensharing.lib.gui;

import lombok.extern.slf4j.Slf4j;
import pl.polsl.screensharing.lib.state.AbstractDisposableProvider;

import javax.swing.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

@Slf4j
public class GuiWindowAdapter extends WindowAdapter {
    private final AbstractRootFrame frame;
    private final AbstractDisposableProvider disposableProvider;
    private final Runnable shutdownHook;

    public GuiWindowAdapter(AbstractRootFrame frame, AbstractDisposableProvider disposableProvider) {
        this(frame, disposableProvider, null);
    }

    public GuiWindowAdapter(
        AbstractRootFrame frame, AbstractDisposableProvider disposableProvider, Runnable shutdownHook
    ) {
        this.frame = frame;
        this.disposableProvider = disposableProvider;
        this.shutdownHook = shutdownHook;
    }

    @Override
    public void windowClosing(WindowEvent e) {
        final int result = JOptionPane.showConfirmDialog(frame, "Are you sure to close app?",
            "Please confirm", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (result == JOptionPane.YES_OPTION) {
            if (shutdownHook != null) {
                try {
                    shutdownHook.run();
                } catch (Exception ex) {
                    log.warn("Error during shutdown: {}", ex.getMessage());
                }
            }
            disposableProvider.disposeAllSubscriptions();
            System.exit(0);
        }
    }
}

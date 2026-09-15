package pl.polsl.screensharing.lib.gui;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pl.polsl.screensharing.lib.UnoperableException;
import pl.polsl.screensharing.lib.Utils;

import javax.swing.*;

@Slf4j
@RequiredArgsConstructor
public abstract class AbstractGUIThread<T> implements Runnable {
    private final T state;

    @Override
    public void run() {
        log.info("Starting GUI thread.");
        try {
            createThreadSaveRootFrame(state);
            log.info("Initialized application GUI.");
        } catch (UnoperableException ex) {
            log.error(ex.getMessage());
            JOptionPane.showMessageDialog(null, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void init() {
        System.setProperty("awt.useSystemAAFontSettings", "on");
        System.setProperty("swing.aatext", "true");
        UiTheme.init();
        Utils.generateThreadUsagePerTick();
        SwingUtilities.invokeLater(this);
    }

    protected abstract void createThreadSaveRootFrame(T state);
}

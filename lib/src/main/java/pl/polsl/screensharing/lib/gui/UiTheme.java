package pl.polsl.screensharing.lib.gui;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import java.awt.*;

public final class UiTheme {
    public static final Color BASE = new Color(0x1E1E1E);
    public static final Color SURFACE = new Color(0x252526);
    public static final Color SURFACE_LIGHT = new Color(0x333337);
    public static final Color BORDER = new Color(0x3C3C3C);
    public static final Color TEXT_PRIMARY = new Color(0xE4E4E4);
    public static final Color TEXT_SECONDARY = new Color(0x9D9D9D);
    public static final Color ACCENT = new Color(0x4FC3F7);
    public static final Color ACCENT_DARK = new Color(0x58A6C7);
    public static final Color SUCCESS = new Color(0x66BB6A);
    public static final Color WARNING = new Color(0xFFCA28);
    public static final Color DANGER = new Color(0xEF5350);

    private static final int BASE_FONT_SIZE = 13;

    public static String hex(Color color) {
        return String.format("#%06x", color.getRGB() & 0xFFFFFF);
    }

    public static String rgb(Color color) {
        final int r = color.getRed();
        final int g = color.getGreen();
        final int b = color.getBlue();
        return String.format("%d, %d, %d", r, g, b);
    }

    private UiTheme() {
    }

    public static void init() {
        applyFontPreferences();
        FlatDarkLaf.setup();
        configureGlobals();
    }

    private static void applyFontPreferences() {
        final String fontName = System.getProperty("os.name", "").toLowerCase().contains("win")
            ? "Segoe UI" : "Dialog";
        final int scaledSize = Math.max(BASE_FONT_SIZE, UiScale.scale(BASE_FONT_SIZE));
        final Font baseFont = UIManager.getFont("Label.font");
        UIManager.put("defaultFont", new Font(fontName, baseFont.getStyle(), scaledSize));
    }

    private static void configureGlobals() {
        UIManager.put("Component.arc", 10);
        UIManager.put("Button.arc", 10);
        UIManager.put("TextComponent.arc", 8);
        UIManager.put("TabbedPane.arc", 8);
        UIManager.put("Component.focusWidth", 1);
        UIManager.put("Component.innerFocusWidth", 0);
        UIManager.put("Component.accentColor", ACCENT);
        UIManager.put("Component.focusColor", new Color(0x66_4F_C3_F7, true));
        UIManager.put("Panel.background", BASE);
        UIManager.put("Panel.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.background", SURFACE_LIGHT);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextArea.background", SURFACE_LIGHT);
        UIManager.put("TextArea.foreground", TEXT_PRIMARY);
        UIManager.put("PasswordField.background", SURFACE_LIGHT);
        UIManager.put("PasswordField.foreground", TEXT_PRIMARY);
        UIManager.put("ComboBox.background", SURFACE_LIGHT);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("Table.background", SURFACE);
        UIManager.put("Table.foreground", TEXT_PRIMARY);
        UIManager.put("Table.selectionBackground", ACCENT_DARK);
        UIManager.put("TabbedPane.background", SURFACE);
        UIManager.put("TabbedPane.foreground", TEXT_PRIMARY);
        UIManager.put("TabbedPane.selectedBackground", SURFACE_LIGHT);
        UIManager.put("TabbedPane.selectedForeground", TEXT_PRIMARY);
        UIManager.put("List.background", SURFACE);
        UIManager.put("List.foreground", TEXT_PRIMARY);
        UIManager.put("ToolBar.background", SURFACE);
        UIManager.put("MenuBar.background", SURFACE);
        UIManager.put("MenuBar.foreground", TEXT_PRIMARY);
        UIManager.put("Menu.background", SURFACE);
        UIManager.put("Menu.foreground", TEXT_PRIMARY);
        UIManager.put("Menu.selectionBackground", SURFACE_LIGHT);
        UIManager.put("Menu.selectionForeground", ACCENT);
        UIManager.put("MenuItem.background", SURFACE);
        UIManager.put("MenuItem.foreground", TEXT_PRIMARY);
        UIManager.put("MenuItem.selectionBackground", SURFACE_LIGHT);
        UIManager.put("MenuItem.selectionForeground", ACCENT);
        UIManager.put("TitledBorder.titleColor", TEXT_SECONDARY);
        UIManager.put("OptionPane.background", BASE);
        UIManager.put("OptionPane.messageForeground", TEXT_PRIMARY);
        UIManager.put("ProgressBar.arc", 10);
        UIManager.put("ScrollBar.width", 12);
        UIManager.put("ScrollPane.background", SURFACE);
        UIManager.put("SplitPane.background", BASE);
    }
}
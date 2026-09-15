package pl.polsl.screensharing.lib.gui;

import java.awt.*;

public final class UiScale {
    private static final double BASE_WIDTH = 1280.0;
    private static final double BASE_HEIGHT = 720.0;
    private static final double MAX_SCALE = 1.5;
    private static final double MIN_SCALE = 1.0;

    private UiScale() {
    }

    public static double getScaleFactor() {
        try {
            final Dimension screen = Toolkit.getDefaultToolkit().getScreenSize();
            final double widthScale = (screen.width * 0.85) / BASE_WIDTH;
            final double heightScale = (screen.height * 0.85) / BASE_HEIGHT;
            final double scale = Math.min(widthScale, heightScale);
            return Math.max(MIN_SCALE, Math.min(scale, MAX_SCALE));
        } catch (HeadlessException ignore) {
            return MIN_SCALE;
        }
    }

    public static int scale(int value) {
        return (int) Math.round(value * getScaleFactor());
    }

    public static double scale(double value) {
        return value * getScaleFactor();
    }

    public static Dimension scale(Dimension dimension) {
        return new Dimension(scale(dimension.width), scale(dimension.height));
    }

    public static Font scaleFont(Font font) {
        return new Font(font.getName(), font.getStyle(), scale(font.getSize()));
    }
}
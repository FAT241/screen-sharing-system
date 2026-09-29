package pl.polsl.screensharing.client.controller;

import lombok.Getter;
import lombok.Setter;
import org.imgscalr.Scalr;
import pl.polsl.screensharing.client.view.fragment.VideoCanvas;
import pl.polsl.screensharing.client.view.tabbed.TabbedVideoStreamPanel;
import pl.polsl.screensharing.lib.SharedConstants;
import pl.polsl.screensharing.lib.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class VideoCanvasController {
    private final VideoCanvas videoCanvas;
    private final TabbedVideoStreamPanel tabbedVideoStreamPanel;

    @Getter
    @Setter
    private volatile BufferedImage receivedImage;

    private double aspectRatio = SharedConstants.DEFAULT_ASPECT_RATIO;

    private BufferedImage scaledImage;
    private BufferedImage scaledSource;
    private int scaledWidth = -1;
    private int scaledHeight = -1;

    public VideoCanvasController(VideoCanvas videoCanvas, TabbedVideoStreamPanel tabbedVideoStreamPanel) {
        this.videoCanvas = videoCanvas;
        this.tabbedVideoStreamPanel = tabbedVideoStreamPanel;
        // the holder has zero size until the window is first laid out, so a resize computed
        // during connect would stick at 0x0 forever. Recompute whenever layout settles.
        tabbedVideoStreamPanel.getVideoStreamHolder().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(final java.awt.event.ComponentEvent e) {
                onResizeWithAspectRatio(aspectRatio);
            }
        });
    }

    public void onResizeWithAspectRatio(double newAspectRatio) {
        final JPanel videoFrameHolder = tabbedVideoStreamPanel.getVideoStreamHolder();
        final Dimension size = Utils
            .calcSizeBaseAspectRatio(videoFrameHolder, newAspectRatio);
        if (videoCanvas == null || tabbedVideoStreamPanel.getConnectionStatusPanel() == null) {
            return;
        }
        // setting a preferred size triggers a resize on the holder, which calls back into here;
        // bailing out on an unchanged size keeps that from becoming a layout loop
        if (size.equals(videoCanvas.getPreferredSize())) {
            aspectRatio = newAspectRatio;
            return;
        }
        aspectRatio = newAspectRatio;
        videoCanvas.setPreferredSize(size);
        tabbedVideoStreamPanel.getConnectionStatusPanel().setPreferredSize(size);
        videoFrameHolder.revalidate();
    }

    public void drawContent(Graphics graphics) {
        final BufferedImage image = receivedImage;
        if (image == null) {
            return;
        }
        int width = videoCanvas.getWidth();
        int height = videoCanvas.getHeight();
        if (width <= 0 || height <= 0) {
            width = videoCanvas.getPreferredSize().width;
            height = videoCanvas.getPreferredSize().height;
        }
        if (width <= 0 || height <= 0) {
            width = image.getWidth();
            height = image.getHeight();
        }
        // resizing a full frame is expensive, and paintComponent runs on every repaint, so
        // only resample when the frame or the target size actually changed
        if (image != scaledSource || width != scaledWidth || height != scaledHeight) {
            scaledImage = Scalr.resize(image, width, height);
            scaledSource = image;
            scaledWidth = width;
            scaledHeight = height;
        }
        graphics.drawImage(scaledImage, 0, 0, videoCanvas);
    }
}

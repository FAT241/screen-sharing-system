package pl.polsl.screensharing.client.controller;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import pl.polsl.screensharing.client.view.fragment.VideoCanvas;
import pl.polsl.screensharing.client.view.tabbed.TabbedVideoStreamPanel;
import pl.polsl.screensharing.lib.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

@Slf4j
public class VideoCanvasController {
    private final VideoCanvas videoCanvas;
    private final TabbedVideoStreamPanel tabbedVideoStreamPanel;

    @Getter
    @Setter
    private volatile BufferedImage receivedImage;

    private int diagCounter;

    public VideoCanvasController(VideoCanvas videoCanvas, TabbedVideoStreamPanel tabbedVideoStreamPanel) {
        this.videoCanvas = videoCanvas;
        this.tabbedVideoStreamPanel = tabbedVideoStreamPanel;
    }

    public void onResizeWithAspectRatio(double aspectRatio) {
        final JPanel videoFrameHolder = tabbedVideoStreamPanel.getVideoStreamHolder();
        final Dimension size = Utils
            .calcSizeBaseAspectRatio(videoFrameHolder, aspectRatio);
        log.info("[DIAG] onResize aspectRatio={} holder={}x{} -> size={}x{}",
            aspectRatio, videoFrameHolder.getWidth(), videoFrameHolder.getHeight(), size.width, size.height);
        if (videoCanvas != null && tabbedVideoStreamPanel.getConnectionStatusPanel() != null) {
            videoCanvas.setPreferredSize(size);
            tabbedVideoStreamPanel.getConnectionStatusPanel().setPreferredSize(size);
            videoFrameHolder.revalidate();
        }
    }

    public void drawContent(Graphics graphics) {
        final Dimension size = videoCanvas.getSize();
        if (++diagCounter % 60 == 0) {
            final BufferedImage img = receivedImage;
            log.info("[DIAG] drawContent canvas={}x{} image={} visible={} bkgd={}",
                size.width, size.height,
                img == null ? "null" : img.getWidth() + "x" + img.getHeight(),
                videoCanvas.isVisible(), videoCanvas.getBackground());
        }
        if (receivedImage != null) {
            if (size.width <= 0 || size.height <= 0) {
                log.warn("[DIAG] canvas has zero size, skipping draw");
                return;
            }
            graphics.drawImage(Scalr.resize(receivedImage, size.width, size.height), 0, 0, videoCanvas);
        }
    }
}

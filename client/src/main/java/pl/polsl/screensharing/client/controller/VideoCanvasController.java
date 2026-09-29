package pl.polsl.screensharing.client.controller;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import pl.polsl.screensharing.client.view.fragment.VideoCanvas;
import pl.polsl.screensharing.client.view.tabbed.TabbedVideoStreamPanel;
import pl.polsl.screensharing.lib.SharedConstants;
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
        // the holder has zero size until the window is first laid out, so a resize computed
        // during connect would stick at 0x0 forever. Recompute whenever layout settles.
        tabbedVideoStreamPanel.getVideoStreamHolder().addComponentListener(new java.awt.event.ComponentAdapter() {
            @Override
            public void componentResized(final java.awt.event.ComponentEvent e) {
                onResizeWithAspectRatio(SharedConstants.DEFAULT_ASPECT_RATIO);
            }
        });
    }

    public void onResizeWithAspectRatio(double aspectRatio) {
        final JPanel videoFrameHolder = tabbedVideoStreamPanel.getVideoStreamHolder();
        final Dimension size = Utils
            .calcSizeBaseAspectRatio(videoFrameHolder, aspectRatio);
        if (videoCanvas == null || tabbedVideoStreamPanel.getConnectionStatusPanel() == null) {
            return;
        }
        // setting a preferred size triggers a resize on the holder, which calls back into here;
        // bailing out on an unchanged size keeps that from becoming a layout loop
        if (size.equals(videoCanvas.getPreferredSize())) {
            return;
        }
        log.info("[DIAG] onResize aspectRatio={} holder={}x{} -> size={}x{}",
            aspectRatio, videoFrameHolder.getWidth(), videoFrameHolder.getHeight(), size.width, size.height);
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
        if (++diagCounter % 60 == 0) {
            log.info("[DIAG] drawContent canvas={}x{} draw={}x{} image={}x{} visible={}",
                videoCanvas.getWidth(), videoCanvas.getHeight(), width, height,
                image.getWidth(), image.getHeight(), videoCanvas.isVisible());
        }
        graphics.drawImage(Scalr.resize(image, width, height), 0, 0, videoCanvas);
    }
}

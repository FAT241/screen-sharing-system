package pl.polsl.screensharing.host.net;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.imgscalr.Scalr;
import pl.polsl.screensharing.host.controller.VideoCanvasController;
import pl.polsl.screensharing.host.state.HostState;
import pl.polsl.screensharing.host.state.QualityLevel;
import pl.polsl.screensharing.host.state.StreamingState;
import pl.polsl.screensharing.host.view.HostWindow;
import pl.polsl.screensharing.lib.UnoperableException;
import pl.polsl.screensharing.lib.net.AbstractDatagramSocketThread;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import javax.swing.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.PortUnreachableException;
import java.net.SocketTimeoutException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

import static pl.polsl.screensharing.lib.SharedConstants.*;

@Slf4j
public class ServerDatagramSocket extends AbstractDatagramSocketThread {
    private int dumpCounter;
    private final HostWindow hostWindow;
    @Getter
    private final HostState hostState;
    private final VideoCanvasController videoCanvasController;
    @Getter
    private final BlockingQueue<byte[]> sendPackagesQueue;
    private final FrameSenderThread frameSenderThread;

    private QualityLevel qualityLevel;
    private boolean isShowing;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public ServerDatagramSocket(HostWindow hostWindow, VideoCanvasController videoCanvasController) {
        super();
        this.hostWindow = hostWindow;
        hostState = hostWindow.getHostState();
        this.videoCanvasController = videoCanvasController;
        qualityLevel = QualityLevel.GOOD;
        sendPackagesQueue = new ArrayBlockingQueue<>(100);
        frameSenderThread = new FrameSenderThread(this);
        initObservables();
    }

    @Override
    public void run() {
        byte[] chunk; // pakiet do przesłania
        int chunkOffset = 0; // przesunięcie pakietowe
        byte[] compressedData = null; // skompresowany strumień bajtów (klatka)
        int unprocessedDataLength = 0; // długość nieprzetworzonych danych
        byte countOfPackages = 0; // liczba przesłanych pakietów na jedną klatkę
        byte packageIteration = 1; // iterator przesłanych pakietów
        final int debugBytesLength = 2; // ilość bajtów debugujących
        final int lengthWithoutIV = PACKAGE_SIZE - debugBytesLength; // długość danych bez IV

        long lastTime = System.nanoTime();
        long currentTime;
        long frameStartTime = System.nanoTime();
        long timer = 0, logTimer = 0;
        long sentBytes = 0;

        // Wątek działa w pętli dopóki istnieje sesja UDP. Kolejne przebiegi pętli to wysyłanie kolejnych to fragmentów
        // jednej klatki obrazu w formie pakietów po N + 2 bajty debugujące definiujące ilość fragmentów na jedną
        // klatkę oraz indeks fragmentu. Wartości te potrzebne są do korekcji błędów po stronie odbiorcy.

        log.info("Started datagram thread with TID {}", getName());
        while (isThreadActive) {
            currentTime = System.nanoTime();
            timer += (currentTime - lastTime);
            logTimer += (currentTime - lastTime);
            lastTime = currentTime;
            try {
                if (compressedData == null) {
                    compressedData = loadImage();
                    dumpFrame(compressedData);
                    unprocessedDataLength = compressedData.length;
                    countOfPackages = (byte) Math.ceil((double) compressedData.length / lengthWithoutIV);
                    frameStartTime = System.nanoTime();
                }
                // przesyłaj pakiety dopóki ilość nieprzetworzonych bajtów będzie większa od rozmiaru ramki bez
                // bajtów debugujących
                if (unprocessedDataLength > lengthWithoutIV) {
                    // prześlij fragment obrazu (jeden pakiet, rozmiar ramki (plus bajty debugujące)
                    chunk = new byte[FRAME_SIZE];
                    chunk[0] = countOfPackages;
                    chunk[1] = packageIteration;

                    // kopiowanie strumienia bajtów JPEG do chunka z przesunięciem o już przetworzone pakiety oraz
                    // 2 pakiety debugujące
                    System.arraycopy(compressedData, chunkOffset, chunk, debugBytesLength, lengthWithoutIV);
                    sendPackagesQueue.put(chunk);

                    sentBytes += FRAME_SIZE;
                    unprocessedDataLength -= lengthWithoutIV;
                    chunkOffset += lengthWithoutIV;
                    packageIteration++;
                } else {
                    // przeslij jeden pakiet (lub ostatni pakiet)
                    chunk = new byte[unprocessedDataLength + debugBytesLength + IV_SIZE];
                    chunk[0] = countOfPackages;
                    chunk[1] = packageIteration;

                    System.arraycopy(compressedData, chunkOffset, chunk, debugBytesLength, unprocessedDataLength);
                    sendPackagesQueue.put(chunk);

                    sentBytes += (unprocessedDataLength + debugBytesLength + IV_SIZE);
                    compressedData = null;
                    chunkOffset = 0;
                    packageIteration = 1;
                }
                sleep(1);
                // Capture and encode run back to back, which floods the link and starves the
                // encoder, so pace the loop to a fixed frame budget.
                final long frameBudget = BILION / TARGET_FPS;
                final long frameElapsed = System.nanoTime() - frameStartTime;
                if (frameElapsed < frameBudget) {
                    sleep((frameBudget - frameElapsed) / 1_000_000L);
                }
            } catch (SocketTimeoutException | PortUnreachableException ex) {
                final String message = ex.getMessage();
                SwingUtilities.invokeLater(
                    () -> JOptionPane.showMessageDialog(hostWindow, message, "Error", JOptionPane.ERROR_MESSAGE));
                log.error("Unexpected network error. Cause: {}", message);
                break;
            } catch (Exception ex) {
                log.warn("Error processing video frame: {}", ex.getMessage());
            }
            if (logTimer >= BILION * 6L) {
                if (sentBytes > 0) {
                    log.info("Host datagram socket processed {} bytes", sentBytes);
                }
                logTimer = 0;
                System.gc();
            }
            if (timer >= BILION) {
                if (isShowing) {
                    hostState.updateSentBytesPerSec(sentBytes);
                }
                log.debug("Host datagram socket processed {} bytes", sentBytes);
                sentBytes = 0;
                timer = 0;
            }
        }
        stopAndClear();
    }

    @Override
    public void createDatagramSocket(byte[] secretKey) {
        try {
            cryptoSymmetricHelper.init(secretKey);
            // host is the UDP sender: bind an ephemeral port instead of advertising a fixed one
            datagramSocket = new DatagramSocket(0);
        } catch (Exception ex) {
            throw new UnoperableException(ex);
        }
    }

    @Override
    protected void abstractStopAndClear() {
        hostState.updateStreamingState(StreamingState.STOPPED);
        hostState.updateRealFpsBuffer(0);
        // without this the sender stays parked in take() forever and leaks on every start/stop cycle
        frameSenderThread.interrupt();
        sendPackagesQueue.clear();
        disposables.dispose();
    }

    @Override
    protected void postStart() {
        frameSenderThread.start();
    }

    private byte[] loadImage() throws IOException {
        final BufferedImage rawImage = videoCanvasController.getRawImage();
        if (rawImage == null) {
            throw new IOException("No screen frame captured yet");
        }
        final BufferedImage scaledImage = fitIntoFrameLimits(rawImage);
        try (
            final ByteArrayOutputStream compressed = new ByteArrayOutputStream();
            final ImageOutputStream outputStream = ImageIO.createImageOutputStream(compressed)
        ) {
            final ImageWriter jpgWriter = ImageIO.getImageWritersByFormatName("JPEG").next();
            try {
                final ImageWriteParam jpgWriteParam = jpgWriter.getDefaultWriteParam();
                jpgWriteParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                jpgWriteParam.setCompressionQuality(qualityLevel.getJpegLevel());
                jpgWriter.setOutput(outputStream);
                jpgWriter.write(null, new IIOImage(scaledImage, null, null), jpgWriteParam);
            } finally {
                jpgWriter.dispose();
            }
            return compressed.toByteArray();
        }
    }

    /**
     * Diagnostic escape hatch: dump the exact JPEG bytes that go on the wire, so a corrupt
     * image can be attributed to capture/encode rather than to the transport.
     * Enable with -Dscreensharing.dumpFrames=true.
     */
    private void dumpFrame(byte[] jpeg) {
        if (!Boolean.getBoolean("screensharing.dumpFrames")) {
            return;
        }
        try {
            final Path dir = Path.of(".logs", "frames");
            Files.createDirectories(dir);
            Files.write(dir.resolve("frame-" + System.currentTimeMillis() + "-" + (dumpCounter++) + ".jpg"), jpeg);
        } catch (IOException ex) {
            log.warn("Could not dump frame: {}", ex.getMessage());
        }
    }

    private BufferedImage fitIntoFrameLimits(BufferedImage rawImage) {
        final double widthScale = (double) MAX_FRAME_WIDTH / rawImage.getWidth();
        final double heightScale = (double) MAX_FRAME_HEIGHT / rawImage.getHeight();
        final double scale = Math.min(1.0, Math.min(widthScale, heightScale));
        if (scale >= 1.0) {
            return rawImage;
        }
        final int newWidth = Math.max(1, (int) (rawImage.getWidth() * scale));
        final int newHeight = Math.max(1, (int) (rawImage.getHeight() * scale));
        return Scalr.resize(rawImage, newWidth, newHeight);
    }

    @Override
    protected void initObservables() {
        disposables.add(hostState.getStreamingQualityLevel$().subscribe(qualityLevel -> {
            this.qualityLevel = qualityLevel;
        }));
        disposables.add(hostState.isScreenIsShowForParticipants$().subscribe(isShowing -> {
            this.isShowing = isShowing;
        }));
    }
}

package pl.polsl.screensharing.client.net;

import lombok.extern.slf4j.Slf4j;
import pl.polsl.screensharing.client.controller.VideoCanvasController;
import pl.polsl.screensharing.client.state.ClientState;
import pl.polsl.screensharing.client.state.ConnectionState;
import pl.polsl.screensharing.client.state.VisibilityState;
import pl.polsl.screensharing.client.view.ClientWindow;
import pl.polsl.screensharing.client.view.fragment.VideoCanvas;
import pl.polsl.screensharing.lib.SharedConstants;
import pl.polsl.screensharing.lib.UnoperableException;
import pl.polsl.screensharing.lib.net.AbstractDatagramSocketThread;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.Arrays;

import static pl.polsl.screensharing.lib.SharedConstants.BILION;
import static pl.polsl.screensharing.lib.SharedConstants.FRAME_SIZE;
import static pl.polsl.screensharing.lib.SharedConstants.IV_SIZE;
import static pl.polsl.screensharing.lib.SharedConstants.PACKAGE_SIZE;

@Slf4j
public class ClientDatagramSocket extends AbstractDatagramSocketThread {
    private final ClientState clientState;
    private final VideoCanvas videoCanvas;
    private final VideoCanvasController videoCanvasController;

    private VisibilityState visibilityState;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final int udpPort;

    public ClientDatagramSocket(
        ClientWindow clientWindow, VideoCanvas videoCanvas, VideoCanvasController videoCanvasController, int udpPort
    ) {
        super();
        clientState = clientWindow.getClientState();
        this.videoCanvas = videoCanvas;
        this.videoCanvasController = videoCanvasController;
        this.udpPort = udpPort;
        visibilityState = VisibilityState.WAITING_FOR_CONNECTION;
        initObservables();
    }

    @Override
    public void run() {
        log.info("Started datagram thread with TID {}", getName());

        final int debugBytesLength = 2; // ilość bajtów debugujących
        final int payloadStride = PACKAGE_SIZE - debugBytesLength;
        // bufor na dane przychodzące (dane + bufor debugujący + IV)
        byte[] receiveBuffer = new byte[FRAME_SIZE + IV_SIZE];
        byte countOfPackages; // liczba pakietów uzyskana przez obiornik
        byte packageIteration; // iterator pakietów uzyskany przez obiornik
        boolean isStarted = false;

        // Klatka składana jest w buforze o stałym rozmiarze, a każdy pakiet trafia na
        // offset wyliczony z jego numeru, a nie dopisywany na końcu. Dopisywanie w kolejności
        // nadania psuło obraz, gdy pakiety przychodziły w innej kolejności - tak się dzieje
        // w sieciach komórkowych, gdzie kolejność UDP nie jest gwarantowana.
        byte[] frameBuffer = null;
        boolean[] receivedFlags = null;
        int receivedPackets = 0;
        int frameDataLength = 0;

        long lastTime = System.nanoTime();
        long currentTime;
        long timer = 0, logTimer = 0;
        long recvBytes = 0;
        int corruptedFrames = 0;

        while (isThreadActive) {
            currentTime = System.nanoTime();
            timer += (currentTime - lastTime);
            logTimer += (currentTime - lastTime);
            lastTime = currentTime;
            try {
                final DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                datagramSocket.receive(receivePacket);
                recvBytes += receivePacket.getLength();

                // odkodowanie danych przy użyciu klucza AES oraz zaszyfrowanego w nim IV (z uwagi na CTR
                final byte[] decrypted = cryptoSymmetricHelper
                    .decrypt(receivePacket.getData(), receivePacket.getLength());

                // przenieś odszyfrowane 3 bajty debugujące do zmiennych
                countOfPackages = decrypted[0];
                packageIteration = decrypted[1];

                // jeśli dołączono w trakcie, ignoruj fragmenty do momentu pierwszego fragmentu klatki
                if (!isStarted) {
                    if (packageIteration == 1) {
                        isStarted = true;
                    } else {
                        continue;
                    }
                }

                // pierwszy fragment klatki oznacza start nowej klatki; poprzednia, jeśli niekompletna, jest
                // porzucana
                if (packageIteration == 1) {
                    if (frameBuffer != null && receivedPackets < countOfPackages) {
                        corruptedFrames++;
                    }
                    final int capacity = countOfPackages * FRAME_SIZE;
                    if (capacity <= 0) {
                        continue;
                    }
                    frameBuffer = new byte[capacity];
                    receivedFlags = new boolean[countOfPackages];
                    receivedPackets = 0;
                    frameDataLength = 0;
                }

                if (frameBuffer == null) {
                    continue;
                }
                final int index = packageIteration - 1;
                if (index < 0 || index >= countOfPackages) {
                    continue;
                }
                // every packet carries a trailing IV-sized pad, which is not image data, and
                // only the last packet is short. The stride is therefore the full payload
                // minus the debug bytes, so consecutive packets stay gapless.
                final int dataLength = decrypted.length - debugBytesLength - IV_SIZE;
                final int offset = index * payloadStride;
                if (dataLength <= 0 || offset + dataLength > frameBuffer.length) {
                    continue; // niespójne z deklarowaną liczbą pakietów
                }
                if (!receivedFlags[index]) {
                    receivedFlags[index] = true;
                    receivedPackets++;
                    frameDataLength = Math.max(frameDataLength, offset + dataLength);
                }

                // klatka gotowa dopiero gdy zebrało się komplet pakietów, niezależnie od kolejności
                if (receivedPackets == countOfPackages) {
                    final BufferedImage image = ImageIO.read(
                        new ByteArrayInputStream(Arrays.copyOf(frameBuffer, frameDataLength)));
                    if (image != null) {
                        videoCanvasController.setReceivedImage(image);
                        videoCanvas.repaint();
                    } else {
                        corruptedFrames++;
                    }
                    frameBuffer = null;
                    receivedFlags = null;
                    receivedPackets = 0;
                    frameDataLength = 0;
                }
            } catch (java.net.SocketTimeoutException ex) {
                log.debug("UDP receive timeout, waiting for data...");
                frameBuffer = null;
                receivedFlags = null;
                receivedPackets = 0;
                frameDataLength = 0;
                isStarted = false;
            } catch (Exception ex) {
                log.warn("Error receiving UDP frame: {}", ex.getMessage());
                frameBuffer = null;
                receivedFlags = null;
                receivedPackets = 0;
                frameDataLength = 0;
            }
            if (logTimer >= BILION * 6L) {
                if (recvBytes > 0) {
                    log.info("Client datagram socket processed {} bytes. Lost frames: {}", recvBytes, corruptedFrames);
                }
                logTimer = 0;
            }
            if (timer >= BILION) {
                clientState.updateRecvBytesPerSec(recvBytes);
                log.debug("Client datagram socket processed {} bytes", recvBytes);
                clientState.updateLostFramesCount(corruptedFrames);
                corruptedFrames = 0;
                recvBytes = 0;
                timer = 0;
            }
        }
        stopAndClear();
    }

    @Override
    public void createDatagramSocket(byte[] secretKey) {
        try {
            cryptoSymmetricHelper.init(secretKey);
            datagramSocket = new DatagramSocket(udpPort);
            datagramSocket.setSoTimeout(1000);
        } catch (Exception ex) {
            clientState.updateConnectionState(ConnectionState.DISCONNECTED);
            throw new UnoperableException(ex);
        }
    }

    @Override
    protected void abstractStopAndClear() {
        if (!visibilityState.equals(VisibilityState.TEMPORARY_HIDDEN)) {
            clientState.updateVisibilityState(VisibilityState.WAITING_FOR_CONNECTION);
        }
        clientState.updateFrameAspectRation(SharedConstants.DEFAULT_ASPECT_RATIO);
        clientState.updateRecvBytesPerSec(0L);
        disposables.dispose();
    }

    @Override
    protected void initObservables() {
        disposables.add(clientState.getVisibilityState$().subscribe(visibilityState -> {
            isThreadActive = visibilityState.equals(VisibilityState.VISIBLE);
            this.visibilityState = visibilityState;
        }));
    }
}

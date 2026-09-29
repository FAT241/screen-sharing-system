package pl.polsl.screensharing.host.net;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import pl.polsl.screensharing.host.view.HostWindow;
import pl.polsl.screensharing.lib.Utils;
import pl.polsl.screensharing.lib.net.CryptoAsymmetricHelper;
import pl.polsl.screensharing.lib.net.SocketState;
import pl.polsl.screensharing.lib.net.payload.KickReason;
import pl.polsl.screensharing.lib.net.payload.SignalState;
import pl.polsl.screensharing.lib.net.payload.VideoFrameDetails;

import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

@Slf4j
public class SendSignalsThread extends Thread {
    private final ClientThread clientThread;
    private final HostWindow hostWindow;
    private final Socket socket;
    private final ObjectMapper objectMapper;
    private final CryptoAsymmetricHelper cryptoAsymmetricHelper;

    @Setter
    private SocketState eventSignalState;

    public SendSignalsThread(ClientThread clientThread, HostWindow hostWindow) {
        this.clientThread = clientThread;
        this.hostWindow = hostWindow;
        socket = clientThread.getSocket();
        cryptoAsymmetricHelper = clientThread.getCryptoAsymmetricHelper();
        eventSignalState = SocketState.WAITING;
        objectMapper = new ObjectMapper();
    }

    /**
     * Gửi tín hiệu ngay trên thread gọi và flush trước khi trả về.
     * Dùng cho các tín hiệu phải chắc chắn tới client trước khi socket bị đóng
     * (END_UP_SESSION, KICK_FROM_SESSION) — không được đi qua cờ async của event loop.
     */
    public void sendSignalNow(SocketState state) {
        try {
            dispatchSignal(state);
        } catch (Exception ex) {
            log.warn("Failed to send {} signal: {}", state, ex.getMessage());
        }
    }

    private void signalEventLoop() throws Exception {
        if (SocketState.WAITING.equals(eventSignalState)) {
            Thread.sleep(50);
            return;
        }
        dispatchSignal(eventSignalState);
    }

    private void dispatchSignal(SocketState state) throws Exception {
        switch (state) {
            case EVENT_START_STREAMING: {
                final VideoFrameDetails videoFrameDetails = VideoFrameDetails.builder()
                    .aspectRatio(Utils.calcAspectRatio(hostWindow.getVideoCanvas().getController().getRawImage()))
                    .streamingSignalState(clientThread.determinateStreamingState())
                    .build();
                performSSLSignal(videoFrameDetails, SocketState.EVENT_START_STREAMING);
                log.info("(signal event) Send start sharing screen event with data {}", videoFrameDetails);
                break;
            }
            case EVENT_STOP_STREAMING: {
                final SignalState<Boolean> signalState = new SignalState<>(true);
                performSSLSignal(signalState, SocketState.EVENT_STOP_STREAMING);
                log.info("(signal event) Send stop sharing screen event with data {}", signalState);
                break;
            }
            case EVENT_TOGGLE_SCREEN_VISIBILITY: {
                final VideoFrameDetails videoFrameDetails = VideoFrameDetails.builder()
                    .aspectRatio(Utils.calcAspectRatio(hostWindow.getVideoCanvas().getController().getRawImage()))
                    .streamingSignalState(clientThread.determinateStreamingState())
                    .build();

                performSSLSignal(videoFrameDetails, SocketState.EVENT_TOGGLE_SCREEN_VISIBILITY);
                log.info("(signal event) Send show/hide screen event with data {}", videoFrameDetails);
                break;
            }
            case KICK_FROM_SESSION: {
                final KickReason kickReason = new KickReason("You has been kicked from session.");
                performSSLSignal(kickReason, SocketState.KICK_FROM_SESSION);
                log.info("(signal event) Send kick user/s event with data {}", kickReason);
                break;
            }
            case END_UP_SESSION: {
                final KickReason kickReason = new KickReason("Session has been ended.");
                performSSLSignal(kickReason, SocketState.END_UP_SESSION);
                log.info("(signal event) Send end up session event with data {}", kickReason);
                break;
            }
            default:
                return;
        }
        eventSignalState = SocketState.WAITING;
        if (SocketState.KICK_FROM_SESSION.equals(state) || SocketState.END_UP_SESSION.equals(state)) {
            closeSocketQuietly();
        }
    }

    private void closeSocketQuietly() {
        try {
            socket.close();
        } catch (IOException ex) {
            log.debug("Socket already closed: {}", ex.getMessage());
        }
    }

    @Override
    public void run() {
        log.info("Started client send events thread with PID {}", getId());
        try {
            while (socket.isConnected() && clientThread.isAlive()) {
                signalEventLoop();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } catch (SocketException ignored) {
        } catch (Exception ex) {
            log.error(ex.getMessage());
        }
        clientThread.stopAndClose();
    }

    @Override
    public synchronized void start() {
        if (!isAlive()) {
            setName("Thread-TCP-Signal-" + clientThread.getThreadId() + getId());
            super.start();
        }
    }

    private void performSSLSignal(Object resData, SocketState signal) throws Exception {
        final String rawResponse = objectMapper.writeValueAsString(resData);
        final String encrypted = cryptoAsymmetricHelper.encrypt(rawResponse, clientThread.getClientPublicKey());
        clientThread.sendLine(signal.generateBody(encrypted));
    }
}

package pl.polsl.screensharing.host.net;

import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@Data
@Builder
@RequiredArgsConstructor
public class ConnectedClientInfo {
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ClientThread clientThread;
    private final String username;
    private final String ipAddress;
    private final int udpPort;
    @Builder.Default
    private final LocalTime joinTime = LocalTime.now();

    public String getFormattedJoinTime() {
        return joinTime.format(TIME_FORMAT);
    }
}

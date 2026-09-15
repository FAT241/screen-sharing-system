package pl.polsl.screensharing.client.state;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pl.polsl.screensharing.lib.gui.UiTheme;
import pl.polsl.screensharing.lib.state.ColoredLabelState;

import java.awt.*;

@Getter
@RequiredArgsConstructor
public enum ConnectionState implements ColoredLabelState {
    CONNECTED("Connected", UiTheme.SUCCESS),
    CONNECTING("Connecting", UiTheme.WARNING),
    DISCONNECTED("Disconnected", UiTheme.TEXT_SECONDARY),
    ;

    private final String state;
    private final Color color;
}

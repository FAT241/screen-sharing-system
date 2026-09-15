package pl.polsl.screensharing.host.state;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pl.polsl.screensharing.lib.gui.UiTheme;
import pl.polsl.screensharing.lib.state.ColoredLabelState;

import java.awt.*;

@Getter
@RequiredArgsConstructor
public enum SessionState implements ColoredLabelState {
    CREATED("Created", UiTheme.ACCENT),
    INACTIVE("Inactive", UiTheme.TEXT_SECONDARY),
    ;

    private final String state;
    private final Color color;
}

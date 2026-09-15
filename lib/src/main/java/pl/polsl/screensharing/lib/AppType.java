package pl.polsl.screensharing.lib;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import pl.polsl.screensharing.lib.file.FileUtils;
import pl.polsl.screensharing.lib.gui.UiScale;

import java.awt.*;
import java.util.Optional;

@Getter
@RequiredArgsConstructor
public enum AppType {
    HOST("HOST", "HostIcon", "host.json"),
    CLIENT("CLIENT", "ClientIcon", "client.json"),
    ;

    private final String rootWindowName;
    private final String iconName;
    private final String configFileName;

    public String getRootWindowTitle() {
        return String.format("%s - Screen Sharing", rootWindowName);
    }

    public Dimension getRootWindowSize() {
        return UiScale.scale(new Dimension(1280, 720));
    }

    public Optional<Image> getIconPath(Class<?> frameClazz) {
        return FileUtils.getRootWindowIconFromResources(frameClazz, this);
    }
}

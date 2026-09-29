# AGENTS.md - screen-sharing-system

Java Swing app (screen sharing via Socket). Project structure and common commands for faster work sessions.

## Environment (this machine)

- JDK 21 at `D:\Java` (javac/java). `JAVA_HOME` is usually NOT set in shell, and `mvn` is NOT on PATH — use the Maven wrapper.
- Use Maven wrapper for builds: `.\mvnw.cmd`.
- Shell is Windows PowerShell 5.1. No `&&`; chain with `;` or `if ($?) { ... }`.
- To build in this session, THREE steps in order (step 3 alone can stay green even when `lib` is broken):
  ```powershell
  .\mvnw.cmd -q -N install
  if ($?) { .\mvnw.cmd -q clean install -pl lib -DskipTests }
  if ($?) { .\mvnw.cmd -q clean package -pl client,host -DskipTests }
  ```
  `-pl client,host` does NOT include `lib` in the reactor, so Maven resolves the **stale `lib` jar from `~/.m2`**.
  The first step (`-N install`) only installs the root POM without building modules or cleaning `.bin`.
  Without it a **fresh machine** fails in step 3: resolving the installed `lib` descriptor needs the root
  POM in `~/.m2`. Do NOT use `-am` for that step — it pulls the root POM into the reactor, which makes
  `clean` wipe `.bin` and fail with "file is being used by another process" while host/client is running.
  `build.cmd` already does all three.

## Build & run (fastest)

- `build.cmd` (root) — auto-detect JDK, rebuild host.jar + client.jar into `.bin/`.
- `start.cmd` — menu: 1=Host, 2=Client, 3=rebuild, 4=exit.
- `start-host.cmd` / `start-client.cmd` — one-click run (auto-builds if jar missing).
- JARs output: `.bin/host.jar`, `.bin/client.jar`.
- Manual: `java -Xms512m -Xmx2g -jar .bin/host.jar` (server), `java -Xms512m -Xmx1g -jar .bin/client.jar`.

## Modules

| Module | Package root | Purpose | Main class |
|--------|-------------|---------|-----------|
| lib | `pl.polsl.screensharing.lib` | shared: GUI base classes, net/crypto, icons, payloads | - |
| host | `pl.polsl.screensharing.host` | server: captures screen, streams to clients | `HostMain` |
| client | `pl.polsl.screensharing.client` | client: receives stream, shows video | `ClientMain` |

Key packages:
- lib: `lib.gui` (AbstractRootFrame, AbstractPopupDialog, AbstractGUIThread, UiScale), `lib.gui.component` (JAppIconButton, JAppTextField...), `lib.icon`, `lib.net`, `lib.utils`.
- host view: `host.view.HostWindow`, `host.view.fragment.*` (TopToolbar, VideoCanvas, settings panels), `host.view.dialog.*`.
- client view: `client.view.ClientWindow`, `client.view.fragment.*`, `client.view.dialog.*`.

## UI conventions (recently modernized)

- `lib.gui.UiScale` — scales dimensions/fonts based on screen size (factor 1.0–1.5). Use for fixed pixel sizes.
- `AppType.getRootWindowSize()` returns scaled 1280x720. Windows are resizable now (min 960x540).
- LAF = system look-and-feel (set in `AbstractGUIThread.init()`), fonts globally scaled.
- Window resize → video canvas uses aspect-ratio logic via `ResizeComponentAdapter` / `Utils.calcSizeBaseAspectRatio`.
- Dialogs use `AbstractPopupDialog` (fixed-size, now DPI-scaled).

## Config / persistence

- Client state → `.bin/client.json`, host state → `.bin/host.json` (Jackson via `PersistedStateLoader`).
- Logs → `.bin/.logs/<module>/<module>.log`, also shown in "Logs" tab.

## Networking summary

- TCP: handshakes/auth/signals. UDP: video datagrams (~48kb frames, encrypted AES).
- Default port 9092 (`SharedConstants.DEFAULT_PORT`).
- Flow: host creates session (TCP) → client connects + auth (password bcrypt) → host streams UDP → client renders.
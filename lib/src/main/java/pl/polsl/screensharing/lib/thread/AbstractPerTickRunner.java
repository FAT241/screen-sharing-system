package pl.polsl.screensharing.lib.thread;

import lombok.extern.slf4j.Slf4j;
import pl.polsl.screensharing.lib.SharedConstants;

@Slf4j
public abstract class AbstractPerTickRunner extends Thread {
    private static final double MAX_FPS = 60.0;
    private volatile boolean isRunning = true;

    @Override
    public void run() {
        final double drawInterval = SharedConstants.BILION / MAX_FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;
        long timer = 0;
        int drawCount = 0;

        while (isRunning) {
            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            timer += (currentTime - lastTime);
            lastTime = currentTime;
            if (delta >= 1) {
                try {
                    onTickUpdate();
                } catch (Exception ex) {
                    // a single bad tick must not kill the render loop silently
                    log.error("Tick update failed: {}", ex.getMessage(), ex);
                }
                delta--;
                drawCount++;
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    break;
                }
            }
            if (timer >= SharedConstants.BILION) {
                onUpdateFpsState(drawCount);
                drawCount = 0;
                timer = 0;
            }
        }
    }

    public void stopRunner() {
        isRunning = false;
        interrupt();
    }

    public abstract void onTickUpdate();

    public abstract void onUpdateFpsState(int fpsValue);
}

package com.github.ykrasik.indexter.games.controller;

import javafx.scene.control.Button;

import java.util.function.Consumer;

/**
 * @author Yevgeny Krasik
 */
public interface UIManager {
    void updateProgress(int current, int total);

    void printMessage(String message);
    void printMessage(String format, Object... args);

    void configureStatusBarStopButton(Consumer<Button> statusBarStopButtonConfigurator);
}

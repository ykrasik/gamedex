package com.github.ykrasik.gamedex.core.controller;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class ControllerProvider {
    @NonNull private final Map<Class<?>, Controller> controllerMap;

    public Controller getController(Class<?> clazz) {
        return Objects.requireNonNull(controllerMap.get(clazz), "Invalid controller class: " + clazz);
    }
}

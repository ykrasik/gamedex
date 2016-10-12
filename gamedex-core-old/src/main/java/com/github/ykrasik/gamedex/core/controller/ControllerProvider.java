package com.github.ykrasik.gamedex.core.controller;

import com.gs.collections.api.map.ImmutableMap;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class ControllerProvider {
    @NonNull private final ImmutableMap<Class<?>, Controller> controllerMap;

    public Controller getController(Class<?> clazz) {
        return Objects.requireNonNull(controllerMap.get(clazz), "Invalid controller class: " + clazz);
    }
}

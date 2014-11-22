package com.github.ykrasik.indexter.util;

import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public final class Optionals {
    private Optionals() {
    }

    public static <T> Optional<T> or(Optional<T> that, Optional<T> other) {
        return that.isPresent() ? that : other;
    }
}

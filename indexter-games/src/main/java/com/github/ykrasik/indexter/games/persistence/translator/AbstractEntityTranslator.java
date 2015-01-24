package com.github.ykrasik.indexter.games.persistence.translator;

import com.github.ykrasik.indexter.id.Id;

/**
 * @author Yevgeny Krasik
 */
public abstract class AbstractEntityTranslator {
    protected <T> Id<T> translateId(int id) {
        return new Id<>(id);
    }
}

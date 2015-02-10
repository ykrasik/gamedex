package com.github.ykrasik.gamedex.persistence.translator;

import com.github.ykrasik.gamedex.datamodel.persistence.Id;

/**
 * @author Yevgeny Krasik
 */
public abstract class AbstractEntityTranslator {
    protected <T> Id<T> translateId(int id) {
        return new Id<>(id);
    }
}

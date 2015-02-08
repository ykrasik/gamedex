package com.github.ykrasik.indexter.games.persistence.translator.exclude;

import com.github.ykrasik.indexter.games.datamodel.persistence.ExcludedPath;
import com.github.ykrasik.indexter.games.persistence.entity.ExcludedPathEntity;

/**
 * @author Yevgeny Krasik
 */
public interface ExcludedPathEntityTranslator {
    ExcludedPath translate(ExcludedPathEntity entity);

    ExcludedPathEntity translate(ExcludedPath excludedPath);
}

package com.github.ykrasik.gamedex.persistence.translator.exclude;

import com.github.ykrasik.gamedex.datamodel.persistence.ExcludedPath;
import com.github.ykrasik.gamedex.persistence.entity.ExcludedPathEntity;

/**
 * @author Yevgeny Krasik
 */
public interface ExcludedPathEntityTranslator {
    ExcludedPath translate(ExcludedPathEntity entity);

    ExcludedPathEntity translate(ExcludedPath excludedPath);
}

package com.github.ykrasik.gamedex.persistence.translator.exclude;

import com.github.ykrasik.gamedex.datamodel.persistence.ExcludedPath;
import com.github.ykrasik.gamedex.persistence.entity.ExcludedPathEntity;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;

import java.nio.file.Paths;

/**
 * @author Yevgeny Krasik
 */
public class ExcludedPathEntityTranslatorImpl implements ExcludedPathEntityTranslator {
    @Override
    public ExcludedPath translate(ExcludedPathEntity entity) {
        return new ExcludedPath(new Id<>(entity.id()), Paths.get(entity.path()));
    }

    @Override
    public ExcludedPathEntity translate(ExcludedPath excludedPath) {
        return new ExcludedPathEntity()
            .id(excludedPath.getId().getId())
            .path(excludedPath.getPath().toString());
    }
}

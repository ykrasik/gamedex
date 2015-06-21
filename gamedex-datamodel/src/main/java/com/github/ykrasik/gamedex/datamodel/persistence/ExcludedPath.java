package com.github.ykrasik.gamedex.datamodel.persistence;

import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
@Value
@EqualsAndHashCode(of = "id")
public class ExcludedPath implements PathEntity {
    @NonNull private final Id<ExcludedPath> id;
    @NonNull private final Path path;

    @Override
    public String toString() {
        return path.toString();
    }
}

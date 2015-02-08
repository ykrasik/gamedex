package com.github.ykrasik.indexter.games.datamodel.persistence;

import com.github.ykrasik.indexter.id.Id;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
@Value
@EqualsAndHashCode(of = "id")
public class ExcludedPath {
    @NonNull private final Id<ExcludedPath> id;
    @NonNull private final Path path;

    @Override
    public String toString() {
        return path.toString();
    }
}

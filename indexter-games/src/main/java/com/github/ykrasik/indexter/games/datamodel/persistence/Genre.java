package com.github.ykrasik.indexter.games.datamodel.persistence;

import com.github.ykrasik.indexter.id.Id;
import lombok.NonNull;
import lombok.Value;

/**
 * @author Yevgeny Krasik
 */
@Value
public class Genre {
    @NonNull private final Id<Genre> id;
    @NonNull private final String name;
}

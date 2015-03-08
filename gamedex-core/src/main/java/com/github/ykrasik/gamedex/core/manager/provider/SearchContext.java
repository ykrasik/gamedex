package com.github.ykrasik.gamedex.core.manager.provider;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.gs.collections.api.set.MutableSet;
import com.gs.collections.impl.set.mutable.UnifiedSet;
import lombok.Data;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.nio.file.Path;
import java.util.Collection;

/**
 * @author Yevgeny Krasik
 */
@Accessors(fluent = true)
@Data
public class SearchContext {
    @Getter private final Path path;
    @Getter private final GamePlatform platform;

    private final MutableSet<String> excludedNames = UnifiedSet.newSet();

    public void addExcludedNames(Iterable<String> excludedNames) {
        this.excludedNames.withAll(excludedNames);
    }

    public Collection<String> getExcludedNames() {
        return excludedNames.asUnmodifiable();
    }
}

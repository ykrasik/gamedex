package com.github.ykrasik.gamedex.core.manager.info;

import com.gs.collections.api.set.MutableSet;
import com.gs.collections.impl.set.mutable.UnifiedSet;

import java.util.Collection;

/**
 * @author Yevgeny Krasik
 */
public class SearchContext {
    private final MutableSet<String> excludedNames = UnifiedSet.newSet();

    public void addExcludedNames(Iterable<String> excludedNames) {
        this.excludedNames.withAll(excludedNames);
    }

    public Collection<String> getExcludedNames() {
        return excludedNames.asUnmodifiable();
    }
}

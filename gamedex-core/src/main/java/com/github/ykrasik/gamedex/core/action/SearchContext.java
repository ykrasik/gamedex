package com.github.ykrasik.gamedex.core.action;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.provider.GameInfoProvider;
import lombok.*;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class SearchContext {
    @NonNull @Getter
    private final GameInfoProvider gameInfoProvider;

    @NonNull @Getter
    private final Path path;

    @NonNull @Getter
    private final GamePlatform platform;

    private final Set<String> excludedNames = new HashSet<>();

    public void addExcludedNames(Collection<String> excludedNames) {
        this.excludedNames.addAll(excludedNames);
    }

    public Collection<String> getExcludedNames() {
        return Collections.unmodifiableSet(excludedNames);
    }
}

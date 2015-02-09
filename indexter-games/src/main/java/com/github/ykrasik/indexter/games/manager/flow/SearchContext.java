package com.github.ykrasik.indexter.games.manager.flow;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.GameInfoService;
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
    private final GameInfoService gameInfoService;

    @NonNull @Getter
    private final Path path;

    @NonNull @Getter
    private final GamePlatform platform;

    @Getter @Setter
    private boolean canExclude = true;

    private final Set<String> excludedNames = new HashSet<>();

    public void addExcludedNames(Collection<String> excludedNames) {
        this.excludedNames.addAll(excludedNames);
    }

    public Collection<String> getExcludedNames() {
        return Collections.unmodifiableSet(excludedNames);
    }
}

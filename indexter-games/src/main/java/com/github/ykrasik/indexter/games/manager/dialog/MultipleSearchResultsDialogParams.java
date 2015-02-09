package com.github.ykrasik.indexter.games.manager.dialog;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.info.SearchResult;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Builder;

import java.nio.file.Path;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
@Value
@Builder
public class MultipleSearchResultsDialogParams {
    @NonNull private final String providerName;
    @NonNull private final String name;
    @NonNull private final GamePlatform platform;
    @NonNull private final Path path;
    @NonNull private final List<SearchResult> searchResults;
    private final boolean canProceedWithout;
}

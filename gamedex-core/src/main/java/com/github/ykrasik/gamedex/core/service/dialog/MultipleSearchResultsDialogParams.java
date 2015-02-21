package com.github.ykrasik.gamedex.core.service.dialog;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.provider.SearchResult;
import com.gs.collections.api.list.ImmutableList;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Accessors;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
@Value
@Accessors(fluent = true)
@Builder
public class MultipleSearchResultsDialogParams {
    @NonNull private final String providerName;
    @NonNull private final String name;
    @NonNull private final GamePlatform platform;
    @NonNull private final Path path;
    @NonNull private final ImmutableList<SearchResult> searchResults;
    private final boolean canProceedWithout;
}

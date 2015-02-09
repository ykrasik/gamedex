package com.github.ykrasik.indexter.games.manager.dialog;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.Builder;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
@Value
@Builder
public class NoSearchResultsDialogParams {
    @NonNull private final String providerName;
    @NonNull private final String name;
    @NonNull private final GamePlatform platform;
    @NonNull private final Path path;
    private final boolean canProceedWithout;
}

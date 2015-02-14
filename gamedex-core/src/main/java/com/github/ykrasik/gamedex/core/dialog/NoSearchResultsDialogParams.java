package com.github.ykrasik.gamedex.core.dialog;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

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

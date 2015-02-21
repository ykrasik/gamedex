package com.github.ykrasik.gamedex.core.service.dialog;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
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
public class NoSearchResultsDialogParams {
    @NonNull private final String providerName;
    @NonNull private final String name;
    @NonNull private final Path path;
    @NonNull private final GamePlatform platform;
    private final boolean canProceedWithout;
}

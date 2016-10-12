package com.github.ykrasik.gamedex.core.ui.library;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import lombok.NonNull;
import lombok.Value;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
@Value
public class LibraryDef {
    @NonNull private final Path path;
    @NonNull private final String name;
    @NonNull private final GamePlatform platform;
}

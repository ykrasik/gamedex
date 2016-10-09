package com.github.ykrasik.gamedex.core.manager.path;

import com.github.ykrasik.gamedex.datamodel.Library;
import com.github.ykrasik.yava.option.Opt;
import lombok.NonNull;
import lombok.Value;

/**
 * @author Yevgeny Krasik
 */
@Value
public class ProcessPathReturnValue {
    @NonNull private final Type type;
    @NonNull private final Opt<Library> createdLibrary;

    public enum Type {
        OK,
        NOT_OK,
        SKIP,
        EXCLUDE,
        NEW_LIBRARY
    }
}

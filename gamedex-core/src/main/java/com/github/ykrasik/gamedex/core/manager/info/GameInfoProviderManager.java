package com.github.ykrasik.gamedex.core.manager.info;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.provider.GameInfo;
import com.github.ykrasik.opt.Opt;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;

import java.nio.file.Path;

/**
 * @author Yevgeny Krasik
 */
public interface GameInfoProviderManager {
    BooleanProperty autoSkipProperty();
    ReadOnlyStringProperty messageProperty();
    ReadOnlyBooleanProperty fetchingProperty();

    Opt<GameInfo> fetchGameInfo(String name, Path path, GamePlatform platform, SearchContext context) throws Exception;
}

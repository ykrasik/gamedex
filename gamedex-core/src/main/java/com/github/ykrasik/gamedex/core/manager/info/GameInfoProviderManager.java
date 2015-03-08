package com.github.ykrasik.gamedex.core.manager.info;

import com.github.ykrasik.gamedex.datamodel.provider.GameInfo;
import com.github.ykrasik.opt.Opt;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.ReadOnlyStringProperty;

/**
 * @author Yevgeny Krasik
 */
public interface GameInfoProviderManager {
    ReadOnlyStringProperty messageProperty();
    ReadOnlyBooleanProperty fetchingProperty();

    Opt<GameInfo> fetchGameInfo(String name, SearchContext context) throws Exception;
}

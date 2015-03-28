package com.github.ykrasik.gamedex.core.manager.provider;

import com.github.ykrasik.gamedex.core.manager.Messageable;
import com.github.ykrasik.gamedex.datamodel.provider.GameInfo;
import com.github.ykrasik.opt.Opt;
import javafx.beans.property.ReadOnlyBooleanProperty;

/**
 * @author Yevgeny Krasik
 */
public interface GameInfoProviderManager extends Messageable {
    ReadOnlyBooleanProperty fetchingProperty();

    Opt<GameInfo> fetchGameInfo(String name, SearchContext context) throws Exception;
}

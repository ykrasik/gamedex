package com.github.ykrasik.gamedex.core.dialog.choice;

import com.github.ykrasik.gamedex.datamodel.info.GameInfo;
import com.github.ykrasik.gamedex.datamodel.info.SearchResult;

import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface DialogChoiceResolver {
    Optional<GameInfo> skip();

    Optional<GameInfo> exclude();

    Optional<GameInfo> proceedAnyway();

    Optional<GameInfo> newName(String newName) throws Exception;

    Optional<GameInfo> choose(SearchResult chosenSearchResult) throws Exception;
}

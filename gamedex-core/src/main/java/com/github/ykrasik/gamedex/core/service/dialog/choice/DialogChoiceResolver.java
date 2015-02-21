package com.github.ykrasik.gamedex.core.service.dialog.choice;

import com.github.ykrasik.gamedex.datamodel.provider.GameInfo;
import com.github.ykrasik.gamedex.datamodel.provider.SearchResult;
import com.github.ykrasik.opt.Opt;

/**
 * @author Yevgeny Krasik
 */
public interface DialogChoiceResolver {
    Opt<GameInfo> skip();

    Opt<GameInfo> exclude();

    Opt<GameInfo> proceedAnyway();

    Opt<GameInfo> newName(String newName) throws Exception;

    Opt<GameInfo> choose(SearchResult chosenSearchResult) throws Exception;
}

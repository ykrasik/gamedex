package com.github.ykrasik.gamedex.core.service.screen.search;

import com.github.ykrasik.gamedex.datamodel.provider.SearchResult;
import com.github.ykrasik.opt.Opt;
import lombok.NonNull;
import lombok.Value;

/**
 * @author Yevgeny Krasik
 */
@Value
public class GameSearchChoice {
    @NonNull private final Opt<SearchResult> searchResult;
    @NonNull private final GameSearchChoiceType type;
}

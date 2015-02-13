package com.github.ykrasik.gamedex.core.dialog.choice;

import com.github.ykrasik.gamedex.datamodel.provider.GameInfo;
import com.github.ykrasik.gamedex.datamodel.provider.SearchResult;
import com.github.ykrasik.opt.Opt;
import lombok.NonNull;
import lombok.Value;

/**
 * @author Yevgeny Krasik
 */
@Value
public class ChooseFromSearchResultsChoice implements DialogChoice {
    @NonNull private final SearchResult searchResult;

    @Override
    public Opt<GameInfo> resolve(DialogChoiceResolver resolver) throws Exception {
        return resolver.choose(searchResult);
    }
}

package com.github.ykrasik.indexter.games.manager.dialog.choice;

import com.github.ykrasik.indexter.games.datamodel.info.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.info.SearchResult;
import lombok.NonNull;
import lombok.Value;

import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
@Value
public class ChooseFromSearchResultsChoice implements DialogChoice {
    @NonNull private final SearchResult searchResult;

    @Override
    public Optional<GameInfo> resolve(DialogChoiceResolver resolver) throws Exception {
        return resolver.choose(searchResult);
    }
}

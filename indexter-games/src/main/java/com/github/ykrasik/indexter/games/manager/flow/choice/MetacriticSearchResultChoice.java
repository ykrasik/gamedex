package com.github.ykrasik.indexter.games.manager.flow.choice;

import com.github.ykrasik.indexter.games.datamodel.info.metacritic.MetacriticSearchResult;
import com.github.ykrasik.indexter.util.Optionals;

import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
public class MetacriticSearchResultChoice {
    private final MetacriticSearchResult searchResult;

    public MetacriticSearchResultChoice(MetacriticSearchResult searchResult) {
        this.searchResult = Objects.requireNonNull(searchResult);
    }

    public MetacriticSearchResult getSearchResult() {
        return searchResult;
    }

    @Override
    public String toString() {
        return String.format(
            "Name: '%s', Release date: %s, Score: %s",
            searchResult.getName(),
            Optionals.toStringOrUnavailable(searchResult.getReleaseDate()),
            Optionals.toStringOrUnavailable(searchResult.getScore())
        );
    }
}

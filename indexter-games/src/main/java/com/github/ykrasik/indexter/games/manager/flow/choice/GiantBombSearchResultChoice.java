package com.github.ykrasik.indexter.games.manager.flow.choice;

import com.github.ykrasik.indexter.games.datamodel.info.giantbomb.GiantBombSearchResult;
import com.github.ykrasik.indexter.util.Optionals;

import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
public class GiantBombSearchResultChoice {
    private final GiantBombSearchResult searchResult;

    public GiantBombSearchResultChoice(GiantBombSearchResult searchResult) {
        this.searchResult = Objects.requireNonNull(searchResult);
    }

    public GiantBombSearchResult getSearchResult() {
        return searchResult;
    }

    @Override
    public String toString() {
        return String.format(
            "Name: '%s', Release date: %s",
            searchResult.getName(),
            Optionals.toStringOrUnavailable(searchResult.getReleaseDate())
        );
    }
}

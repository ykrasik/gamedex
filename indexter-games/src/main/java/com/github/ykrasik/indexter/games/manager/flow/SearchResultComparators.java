package com.github.ykrasik.indexter.games.manager.flow;

import com.github.ykrasik.indexter.games.datamodel.info.SearchResult;
import com.github.ykrasik.indexter.optional.OptionalComparators;

import java.util.Comparator;
import java.util.Optional;
import java.util.function.Function;

/**
 * @author Yevgeny Krasik
 */
public final class SearchResultComparators {
    private SearchResultComparators() { }

    private static final Comparator<SearchResult> NAME_COMPARATOR = (o1, o2) ->
        o1.getName().compareTo(o2.getName());

    private static final Comparator<SearchResult> RELEASE_DATE_COMPARATOR = (o1, o2) ->
        compareWithNameFallback(o1, o2, SearchResult::getReleaseDate);

    public static Comparator<SearchResult> nameComparator() {
        return NAME_COMPARATOR;
    }

    public static Comparator<SearchResult> releaseDateComparator() {
        return RELEASE_DATE_COMPARATOR;
    }

    private static <T extends Comparable<? super T>> int compareWithNameFallback(SearchResult o1, SearchResult o2, Function<SearchResult, Optional<T>> fieldExtractor) {
        return OptionalComparators.compareWithFallback(o2, o1, fieldExtractor, NAME_COMPARATOR);
    }
}

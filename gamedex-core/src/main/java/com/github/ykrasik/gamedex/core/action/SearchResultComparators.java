package com.github.ykrasik.gamedex.core.action;

import com.github.ykrasik.gamedex.common.comparator.FieldComparator;
import com.github.ykrasik.gamedex.common.comparator.OptionalComparator;
import com.github.ykrasik.gamedex.datamodel.provider.SearchResult;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Comparator;

/**
 * @author Yevgeny Krasik
 */
@Accessors(fluent = true)
public final class SearchResultComparators {
    private SearchResultComparators() { }

    @Getter private static final Comparator<SearchResult> nameAsc = FieldComparator.of(SearchResult::getName);
    @Getter private static final Comparator<SearchResult> nameDesc = nameAsc.reversed();

    @Getter private static final Comparator<SearchResult> releaseDateAsc = OptionalComparator.of(SearchResult::getReleaseDate).or(nameAsc);
    @Getter private static final Comparator<SearchResult> releaseDateDesc = releaseDateAsc.reversed();
}

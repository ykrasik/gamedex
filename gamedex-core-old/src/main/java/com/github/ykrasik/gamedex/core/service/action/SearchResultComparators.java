package com.github.ykrasik.gamedex.core.service.action;

import com.github.ykrasik.gamedex.core.comparator.FieldComparator;
import com.github.ykrasik.gamedex.core.comparator.OptionalComparator;
import com.gitlab.ykrasik.gamedex.provider.ProviderSearchResult;
import lombok.Getter;
import lombok.experimental.Accessors;

import java.util.Comparator;

/**
 * @author Yevgeny Krasik
 */
@Accessors(fluent = true)
public final class SearchResultComparators {
    private SearchResultComparators() { }

    @Getter private static final Comparator<ProviderSearchResult> nameAsc = FieldComparator.of(ProviderSearchResult::getName);
    @Getter private static final Comparator<ProviderSearchResult> nameDesc = nameAsc.reversed();

    @Getter private static final Comparator<ProviderSearchResult> releaseDateAsc = OptionalComparator.of(ProviderSearchResult::getReleaseDate).thenComparing(nameAsc);
    @Getter private static final Comparator<ProviderSearchResult> releaseDateDesc = releaseDateAsc.reversed();
}

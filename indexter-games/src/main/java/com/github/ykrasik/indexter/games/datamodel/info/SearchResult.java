package com.github.ykrasik.indexter.games.datamodel.info;

import java.time.LocalDate;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface SearchResult {
    String getName();
    Optional<LocalDate> getReleaseDate();
    Optional<Double> getScore();

    String getDetailUrl();
}

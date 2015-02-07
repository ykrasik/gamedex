package com.github.ykrasik.indexter.games.datamodel.info;

import java.time.LocalDate;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public interface GameInfo {
    String getName();
    Optional<String> getDescription();
    Optional<LocalDate> getReleaseDate();
    Optional<Double> getCriticScore();
    Optional<Double> getUserScore();
}

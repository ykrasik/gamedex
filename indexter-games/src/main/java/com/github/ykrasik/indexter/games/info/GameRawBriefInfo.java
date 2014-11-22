package com.github.ykrasik.indexter.games.info;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class GameRawBriefInfo {
    private final String name;
    private final Optional<LocalDate> releaseDate;
    private final Optional<Double> score;    // FIXME: Should this be here in the brief? Giantbomb doesn't provide it.
    private final Optional<String> tinyImageUrl;
    private final Optional<String> giantBombApiDetailUrl;

    public GameRawBriefInfo(String name,
                            Optional<LocalDate> releaseDate,
                            Optional<Double> score,
                            Optional<String> tinyImageUrl,
                            Optional<String> giantBombApiDetailUrl) {
        this.name = Objects.requireNonNull(name);
        this.releaseDate = Objects.requireNonNull(releaseDate);
        this.score = Objects.requireNonNull(score);
        this.tinyImageUrl = Objects.requireNonNull(tinyImageUrl);
        this.giantBombApiDetailUrl = Objects.requireNonNull(giantBombApiDetailUrl);
    }

    public String getName() {
        return name;
    }

    public Optional<LocalDate> getReleaseDate() {
        return releaseDate;
    }

    public Optional<Double> getScore() {
        return score;
    }

    public Optional<String> getTinyImageUrl() {
        return tinyImageUrl;
    }

    public Optional<String> getGiantBombApiDetailUrl() {
        return giantBombApiDetailUrl;
    }

    // FIXME: Make a real toString and don't use directly as a choice.
    @Override
    public String toString() {
        return String.format(
            "Name: '%s', Release date: %s, Score: %s",
            name, releaseDate.map(Object::toString).orElse("Unavailable"), score.map(String::valueOf).orElse("Unavailable")
        );
    }
}

package com.github.ykrasik.indexter.games.info;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class GameRawBriefInfo {
    private final String name;
    private final GamePlatform gamePlatform;
    private final Optional<LocalDate> releaseDate;
    private final double score;    // FIXME: Should this be here in the brief? Giantbomb doesn't provide it.
    private final Optional<String> thumbnailUrl;
    private final Optional<String> tinyImageUrl;
    private final String moreDetailsId;

    public GameRawBriefInfo(String name,
                            GamePlatform gamePlatform,
                            Optional<LocalDate> releaseDate,
                            double score,
                            Optional<String> thumbnailUrl,
                            Optional<String> tinyImageUrl,
                            String moreDetailsId) {
        this.name = Objects.requireNonNull(name);
        this.gamePlatform = Objects.requireNonNull(gamePlatform);
        this.releaseDate = Objects.requireNonNull(releaseDate);
        this.score = score;
        this.thumbnailUrl = Objects.requireNonNull(thumbnailUrl);
        this.tinyImageUrl = Objects.requireNonNull(tinyImageUrl);
        this.moreDetailsId = Objects.requireNonNull(moreDetailsId);
    }

    public String getName() {
        return name;
    }

    public GamePlatform getGamePlatform() {
        return gamePlatform;
    }

    public Optional<LocalDate> getReleaseDate() {
        return releaseDate;
    }

    public double getScore() {
        return score;
    }

    public Optional<String> getThumbnailUrl() {
        return thumbnailUrl;
    }

    public Optional<String> getTinyImageUrl() {
        return tinyImageUrl;
    }

    public String getMoreDetailsId() {
        return moreDetailsId;
    }

    @Override
    public String toString() {
        return String.format(
            "Name: '%s', Release date: %s, Score: %s",
            name, releaseDate.map(Object::toString).orElse("Not Available"), score
        );
    }
}

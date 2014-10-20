package com.github.ykrasik.indexter.games.datamodel;

import com.google.common.base.MoreObjects;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class GameBriefInfo {
    private final String name;
    private final GamePlatform gamePlatform;
    private final Optional<LocalDate> releaseDate;
    private final double score;    // FIXME: Should this be here in the brief? Giantbomb doesn't provide it.
    private final Optional<String> imageUrl;
    private final String moreDetailsId;

    public GameBriefInfo(String name,
                         GamePlatform gamePlatform,
                         Optional<LocalDate> releaseDate,
                         double score,
                         Optional<String> imageUrl,
                         String moreDetailsId) {
        this.name = Objects.requireNonNull(name);
        this.gamePlatform = Objects.requireNonNull(gamePlatform);
        this.releaseDate = Objects.requireNonNull(releaseDate);
        this.score = score;
        this.imageUrl = Objects.requireNonNull(imageUrl);
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

    public Optional<String> getImageUrl() {
        return imageUrl;
    }

    public String getMoreDetailsId() {
        return moreDetailsId;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("name", name)
            .add("platform", gamePlatform)
            .add("releaseDate", releaseDate)
            .add("score", score)
            .add("imageUrl", imageUrl)
            .add("moreDetailsId", moreDetailsId)
            .toString();
    }
}

package com.github.ykrasik.indexter.games.datamodel;

import com.google.common.base.MoreObjects;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class GameDetailedInfo {
    private final String name;
    private final Optional<String> description;
    private final GamePlatform gamePlatform;
    private final Optional<LocalDate> releaseDate;
    private final double criticScore;
    private final double userScore;
    private final List<String> genres;
    private final List<String> publishers;
    private final List<String> developers;
    private final String url;
    private final String thumbnailUrl;

    public GameDetailedInfo(String name,
                            Optional<String> description,
                            GamePlatform gamePlatform,
                            Optional<LocalDate> releaseDate,
                            double criticScore,
                            double userScore,
                            List<String> genres,
                            List<String> publishers,
                            List<String> developers,
                            String url,
                            String thumbnailUrl) {
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.gamePlatform = Objects.requireNonNull(gamePlatform);
        this.releaseDate = Objects.requireNonNull(releaseDate);
        this.criticScore = criticScore;
        this.userScore = userScore;
        this.genres = Objects.requireNonNull(genres);
        this.publishers = Objects.requireNonNull(publishers);
        this.developers = Objects.requireNonNull(developers);
        this.url = Objects.requireNonNull(url);
        this.thumbnailUrl = Objects.requireNonNull(thumbnailUrl);
    }

    public String getName() {
        return name;
    }

    public Optional<String> getDescription() {
        return description;
    }

    public GamePlatform getGamePlatform() {
        return gamePlatform;
    }

    public Optional<LocalDate> getReleaseDate() {
        return releaseDate;
    }

    public double getCriticScore() {
        return criticScore;
    }

    public double getUserScore() {
        return userScore;
    }

    public List<String> getGenre() {
        return genres;
    }

    public List<String> getPublishers() {
        return publishers;
    }

    public List<String> getDevelopers() {
        return developers;
    }

    public String getUrl() {
        return url;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("name", name)
            .add("description", description)
            .add("platform", gamePlatform)
            .add("releaseDate", releaseDate)
            .add("criticScore", criticScore)
            .add("userScore", userScore)
            .add("genres", genres)
            .add("publishers", publishers)
            .add("developers", developers)
            .add("url", url)
            .add("thumbnailUrl", thumbnailUrl)
            .toString();
    }
}

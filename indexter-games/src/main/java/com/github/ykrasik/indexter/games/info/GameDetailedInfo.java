package com.github.ykrasik.indexter.games.info;

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
    private final Platform platform;
    private final Optional<LocalDate> releaseDate;
    private final double criticScore;
    private final double userScore;
    private final List<String> genres;
    private final String publisher;
    private final String developer;
    private final String url;
    private final String thumbnailUrl;

    public GameDetailedInfo(String name,
                            Optional<String> description,
                            Platform platform,
                            Optional<LocalDate> releaseDate,
                            double criticScore,
                            double userScore,
                            List<String> genres,
                            String publisher,
                            String developer,
                            String url,
                            String thumbnailUrl) {
        this.name = Objects.requireNonNull(name);
        this.description = Objects.requireNonNull(description);
        this.platform = Objects.requireNonNull(platform);
        this.releaseDate = Objects.requireNonNull(releaseDate);
        this.criticScore = criticScore;
        this.userScore = userScore;
        this.genres = Objects.requireNonNull(genres);
        this.publisher = Objects.requireNonNull(publisher);
        this.developer = Objects.requireNonNull(developer);
        this.url = Objects.requireNonNull(url);
        this.thumbnailUrl = Objects.requireNonNull(thumbnailUrl);
    }

    public String getName() {
        return name;
    }

    public Optional<String> getDescription() {
        return description;
    }

    public Platform getPlatform() {
        return platform;
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

    public String getPublisher() {
        return publisher;
    }

    public String getDeveloper() {
        return developer;
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
            .add("platform", platform)
            .add("releaseDate", releaseDate)
            .add("criticScore", criticScore)
            .add("userScore", userScore)
            .add("genres", genres)
            .add("publisher", publisher)
            .add("developer", developer)
            .add("url", url)
            .add("thumbnailUrl", thumbnailUrl)
            .toString();
    }
}

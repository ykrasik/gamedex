package com.github.ykrasik.indexter.games.datamodel;

import com.github.ykrasik.indexter.util.UrlUtils;
import com.google.common.base.MoreObjects;
import javafx.scene.image.Image;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class GameInfo {
    private final String name;
    private final GamePlatform platform;

    private final Optional<String> description;
    private final Optional<LocalDate> releaseDate;
    private final Optional<Double> criticScore;
    private final Optional<Double> userScore;

    private final List<String> genres;

    private final Optional<String> giantBombApiDetailsUrl;

    private final Optional<byte[]> thumbnailData;
    private final Optional<Image> thumbnail;

    private final Optional<byte[]> posterData;
    private final Optional<Image> poster;

    public GameInfo(String name,
                    GamePlatform platform,
                    Optional<String> description,
                    Optional<LocalDate> releaseDate,
                    Optional<Double> criticScore,
                    Optional<Double> userScore,
                    List<String> genres,
                    Optional<String> giantBombApiDetailsUrl,
                    Optional<byte[]> thumbnailData,
                    Optional<byte[]> posterData) {
        this.name = Objects.requireNonNull(name);
        this.platform = Objects.requireNonNull(platform);
        this.description = Objects.requireNonNull(description);
        this.releaseDate = Objects.requireNonNull(releaseDate);
        this.criticScore = Objects.requireNonNull(criticScore);
        this.userScore = Objects.requireNonNull(userScore);
        this.genres = Objects.requireNonNull(genres);
        this.giantBombApiDetailsUrl = Objects.requireNonNull(giantBombApiDetailsUrl);
        this.thumbnailData = Objects.requireNonNull(thumbnailData);
        this.posterData = Objects.requireNonNull(posterData);

        this.thumbnail = createImage(thumbnailData);
        this.poster = createImage(posterData);
    }

    private Optional<Image> createImage(Optional<byte[]> bytes) {
        // There is no need to close byte array input streams.
        return bytes.map(data -> new Image(new ByteArrayInputStream(data)));
    }

    public String getName() {
        return name;
    }

    public GamePlatform getPlatform() {
        return platform;
    }

    public Optional<String> getDescription() {
        return description;
    }

    public Optional<LocalDate> getReleaseDate() {
        return releaseDate;
    }

    public Optional<Double> getCriticScore() {
        return criticScore;
    }

    public Optional<Double> getUserScore() {
        return userScore;
    }

    public List<String> getGenres() {
        return genres;
    }

    public Optional<String> getGiantBombApiDetailsUrl() {
        return giantBombApiDetailsUrl;
    }

    public Optional<byte[]> getThumbnailData() {
        return thumbnailData;
    }

    public Optional<Image> getThumbnail() {
        return thumbnail;
    }

    public Optional<byte[]> getPosterData() {
        return posterData;
    }

    public Optional<Image> getPoster() {
        return poster;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
            .add("name", name)
            .add("platform", platform)
            .toString();
    }

    public static GameInfo from(String name,
                                GamePlatform platform,
                                Optional<String> description,
                                Optional<LocalDate> releaseDate,
                                Optional<Double> criticScore,
                                Optional<Double> userScore,
                                List<String> genres,
                                Optional<String> giantBombApiDetailsUrl,
                                Optional<String> thumbnailUrl,
                                Optional<String> posterUrl) throws IOException {
        final Optional<byte[]> thumbnailData = fetchUrl(thumbnailUrl);
        final Optional<byte[]> posterData = fetchUrl(posterUrl);

        return new GameInfo(
            name, platform, description, releaseDate, criticScore, userScore, genres, giantBombApiDetailsUrl,
            thumbnailData, posterData
        );
    }

    private static Optional<byte[]> fetchUrl(Optional<String> url) throws IOException {
        if (url.isPresent()) {
            return Optional.of(UrlUtils.fetchData(url.get()));
        } else {
            return Optional.empty();
        }
    }
}

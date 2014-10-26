package com.github.ykrasik.indexter.games.info.provider.metacritic.translator;

import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.GameRawBriefInfo;
import com.github.ykrasik.indexter.json.AbstractJsonTranslator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class MetacriticTranslator extends AbstractJsonTranslator {
    private final JsonNode root;
    private final GamePlatform platform;

    public MetacriticTranslator(ObjectMapper mapper, String rawBody, GamePlatform platform) throws IOException {
        this.root = Objects.requireNonNull(mapper).readTree(Objects.requireNonNull(rawBody));
        this.platform = Objects.requireNonNull(platform);
    }

    public List<GameRawBriefInfo> translateBriefInfos() {
        final JsonNode results = getMandatoryField(root, "results");
        return mapList(results, this::translateGameBriefInfo);
    }

    private GameRawBriefInfo translateGameBriefInfo(JsonNode node) {
        final String name = extractName(node);
        final Optional<LocalDate> releaseDate = extractReleaseDate(node);
        final double score = extractDouble(node, "score").orElse(0.0);

        // Metacritic API doesn't provide a thumbnail or tinyImage on brief.
        final Optional<String> thumbnailUrl = Optional.<String>empty();
        final Optional<String> tinyImageUrl = Optional.<String>empty();

        // Metacritic API fetches more details by name.
        final String moreDetailsId = name;

        return new GameRawBriefInfo(name, platform, releaseDate, score, thumbnailUrl, tinyImageUrl, moreDetailsId);
    }

    public Optional<GameInfo> translateGameInfo() throws IOException {
        final JsonNode node = getMandatoryField(root, "result");
        if (node.isBoolean() && !node.asBoolean()) {
            return Optional.empty();
        }

        final String name = extractName(node);

        // Metacritic API does not provide description.
        final Optional<String> description = Optional.empty();

        final Optional<LocalDate> releaseDate = extractReleaseDate(node);
        final double criticScore = extractDouble(node, "score").orElse(0.0);
        final double userscore = extractDouble(node, "userscore").orElse(0.0);

        // Only 1 genre from Metacritic API.
        final List<String> genres = toList(extractString(node, "genre"));

        // Only 1 publisher from Metacritic API
        final List<String> publishers = toList(extractString(node, "publisher"));

        // Only 1 developer from Metacritic API
        final List<String> developers = toList(extractString(node, "developer"));

        final Optional<String> url = extractString(node, "url");
        final Optional<String> thumbnailUrl = extractString(node, "thumbnail");

        return Optional.of(GameInfo.from(
            name, description, platform, releaseDate, criticScore, userscore,
            genres, publishers, developers, url, thumbnailUrl
        ));
    }

    private String extractName(JsonNode node) {
        return getMandatoryField(node, "name").asText();
    }

    private Optional<LocalDate> extractReleaseDate(JsonNode node) {
        final Optional<String> releaseDate = extractString(node, "rlsdate");
        return releaseDate.flatMap(this::translateDate);
    }

    private Optional<LocalDate> translateDate(String raw) {
        try {
            return Optional.of(LocalDate.parse(raw));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private <T> List<T> toList(Optional<T> optional) {
        if (optional.isPresent()) {
            return Collections.singletonList(optional.get());
        } else {
            return Collections.emptyList();
        }
    }
}

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
// FIXME: This style sucks, move this code back into the service
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
        final Optional<Double> score = extractDouble(node, "score");

        // Metacritic API doesn't provide a tinyImage on brief.
        final Optional<String> tinyImageUrl = Optional.<String>empty();

        // Metacritic API fetches more details by name.
        final Optional<String> giantBombApiDetailUrl = Optional.empty();

        return new GameRawBriefInfo(name, releaseDate, score, tinyImageUrl, giantBombApiDetailUrl);
    }

    public Optional<GameInfo> translateGameInfo() throws IOException {
        final JsonNode node = getMandatoryField(root, "result");
        if (node.isBoolean() && !node.asBoolean()) {
            return Optional.empty();
        }

        final String name = extractName(node);

        Optional<String> description = extractString(node, "summary").map(this::removeLastExpand);
        description = description.filter(desc -> !desc.isEmpty());

        final Optional<LocalDate> releaseDate = extractReleaseDate(node);
        Optional<Double> criticScore = extractDouble(node, "score");
        criticScore = criticScore.filter(score -> score != 0.0);

        // userScore is on a scale of 1-10, while for some reason criticScore is on a scale of 1-100.
        Optional<Double> userScore = extractDouble(node, "userscore").map(score -> score * 10);
        userScore = userScore.filter(score -> score != 0.0);

        // Only 1 genre from Metacritic API.
        final List<String> genres = toList(extractString(node, "genre"));
        final Optional<String> giantBombApiDetailUrl = Optional.empty();

        Optional<String> thumbnailUrl = extractString(node, "thumbnail");
        thumbnailUrl = thumbnailUrl.filter(url -> !"http://static.metacritic.com/images/products/games/98w-game.jpg".equals(url));

        // No poster from Metacritic API.
        final Optional<String> posterUrl = Optional.empty();

        return Optional.of(GameInfo.from(
            name, platform, description, releaseDate, criticScore, userScore, genres, giantBombApiDetailUrl,
            thumbnailUrl, posterUrl
        ));
    }

    private String removeLastExpand(String str) {
        // Remove the last "expand" word that Metacritic API adds to the description
        final int index = str.lastIndexOf("… Expand");
        return index != -1 ? str.substring(0, index).trim() : str;
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

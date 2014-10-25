package com.github.ykrasik.indexter.games.info.provider.giantbomb.translator;

import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.GameRawBriefInfo;
import com.github.ykrasik.indexter.json.AbstractJsonTranslator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class GiantBombTranslator extends AbstractJsonTranslator {
    private final JsonNode root;
    private final GamePlatform platform;

    public GiantBombTranslator(ObjectMapper mapper, String rawBody, GamePlatform platform) throws IOException {
        this.root = Objects.requireNonNull(mapper).readTree(Objects.requireNonNull(rawBody));
        this.platform = Objects.requireNonNull(platform);
    }

    public int getStatusCode() {
        return getMandatoryField(root, "status_code").asInt();
    }

    public List<GameRawBriefInfo> translateBriefInfos() {
        final JsonNode results = getResults();
        return mapList(results, this::translateGameBriefInfo);
    }

    private GameRawBriefInfo translateGameBriefInfo(JsonNode node) {
        final String name = extractName(node);
        final Optional<LocalDate> releaseDate = extractReleaseDate(node);

        // GiantBomb API doesn't provide score for brief.
        final double score = 0.0;

        final Optional<String> thumbnailUrl = extractThumbnailUrl(node);
        final Optional<String> tinyImageUrl = extractTinyImageUrl(node);

        // The GiantBomb API fetches more details by an api_detail_url field.
        final String moreDetailsId = getMandatoryField(node, "api_detail_url").asText();

        return new GameRawBriefInfo(name, platform, releaseDate, score, thumbnailUrl, tinyImageUrl, moreDetailsId);
    }

    public GameInfo translateGameInfo() throws IOException {
        final JsonNode node = getResults();

        final String name = extractName(node);
        final Optional<String> description = extractString(node, "deck");
        final Optional<LocalDate> releaseDate = extractReleaseDate(node);

        // FIXME: Collect review scores in another API call.
        final double criticScore = 0.0;
        final double userScore = 0.0;

        final List<String> genres = extractListOfFields(node.get("genres"), "name");
        final List<String> publishers = extractListOfFields(node.get("publishers"), "name");
        final List<String> developers = extractListOfFields(node.get("developers"), "name");
        final Optional<String> url = extractString(node, "site_detail_url");
        final Optional<String> thumbnailUrl = extractThumbnailUrl(node);

        return GameInfo.from(
            name, description, platform, releaseDate, criticScore, userScore,
            genres, publishers, developers, url, thumbnailUrl
        );
    }

    private JsonNode getResults() {
        return getMandatoryField(root, "results");
    }

    private String extractName(JsonNode node) {
        return getMandatoryField(node, "name").asText();
    }

    private Optional<LocalDate> extractReleaseDate(JsonNode node) {
        final Optional<String> originalReleaseDate = extractString(node, "original_release_date");
        return originalReleaseDate.flatMap(this::translateDate);
    }

    private Optional<LocalDate> translateDate(String raw) {
        // The date comes at a non-standard format, with a ' ' between the date and time (rather then 'T' as ISO dictates).
        // We don't care about the time anyway, just parse the date.
        try {
            final int indexOfSpace = raw.indexOf(' ');
            final String toParse = indexOfSpace != -1 ? raw.substring(0, indexOfSpace) : raw;
            return Optional.of(LocalDate.parse(toParse));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private Optional<String> extractThumbnailUrl(JsonNode node) {
        return extractImageUrl(node, "thumb_url");
    }

    private Optional<String> extractTinyImageUrl(JsonNode node) {
        return extractImageUrl(node, "tiny_url");
    }

    private Optional<String> extractImageUrl(JsonNode node, String imageName) {
        final Optional<JsonNode> image = getField(node, "image");
        return image.flatMap(imageNode -> extractString(imageNode, imageName));
    }

    private List<String> extractListOfFields(JsonNode root, String fieldName) {
        return flatMapList(root, node -> extractString(node, fieldName));
    }
}

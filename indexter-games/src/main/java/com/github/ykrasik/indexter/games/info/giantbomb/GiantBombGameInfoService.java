package com.github.ykrasik.indexter.games.info.giantbomb;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.games.datamodel.GameBriefInfo;
import com.github.ykrasik.indexter.games.datamodel.GameDetailedInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.info.giantbomb.client.GiantBombGameInfoClient;
import com.github.ykrasik.indexter.games.info.giantbomb.config.GiantBombProperties;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * @author Yevgeny Krasik
 */
public class GiantBombGameInfoService extends AbstractService implements GameInfoService {
    private final GiantBombGameInfoClient client;
    private final GiantBombProperties properties;
    private final ObjectMapper objectMapper;

    public GiantBombGameInfoService(GiantBombGameInfoClient client,
                                    GiantBombProperties properties,
                                    ObjectMapper objectMapper) {
        this.client = Objects.requireNonNull(client);
        this.properties = Objects.requireNonNull(properties);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected void doStop() throws Exception {

    }

    @Override
    public List<GameBriefInfo> searchGames(String name, GamePlatform gamePlatform) throws Exception {
        LOG.info("Searching for name={}, platform={}...", name, gamePlatform);
        final int platformId = properties.getPlatformId(gamePlatform);
        final String rawBody = client.searchGames(name, platformId);
        LOG.debug("rawBody={}", rawBody);

        final JsonNode root = objectMapper.readTree(rawBody);
        if (root.get("status_code").asInt() != 1) {
            throw new RuntimeException("Error executing request: searchGames. name=" + name + ", platform=" + gamePlatform);
        }

        final List<GameBriefInfo> infos = new ArrayList<>();
        final Iterator<JsonNode> iterator = root.get("results").getElements();
        while (iterator.hasNext()) {
            final JsonNode node = iterator.next();
            infos.add(translateGameBriefInfo(node, gamePlatform));
        }

        LOG.info("Found {} results.", infos.size());
        return infos;
    }

    private GameBriefInfo translateGameBriefInfo(JsonNode node, GamePlatform gamePlatform) {
        final String name = node.get("name").asText();
        final Optional<LocalDate> releaseDate = translateDate(node.get("original_release_date").asText());

        // GiantBomb API doesn't provide reviews for brief.
        final double score = 0.0;

        final Optional<String> thumbnailUrl = Optional.of(extractThumbnailUrl(node));

        // The GiantBomb API fetches more details by an api_detail_url field.
        final String moreDetailsId = node.get("api_detail_url").asText();

        return new GameBriefInfo(
            name,
            gamePlatform,
            releaseDate,
            score,
            thumbnailUrl,
            moreDetailsId
        );
    }

    private Optional<LocalDate> translateDate(String raw) {
        // The date comes at a non-standard format, with a ' ' between the date and time (rather then 'T' as ISO dictates).
        // We don't care about the time anyway, just parse the date.
        try {
            final int indexOfSpace = raw.indexOf(' ');
            final String toParse = indexOfSpace != -1 ? raw.substring(0, indexOfSpace) : raw;
            final LocalDate parsed = LocalDate.parse(toParse);
            return Optional.of(parsed);
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<GameDetailedInfo> getDetails(String apiDetailUrl, GamePlatform gamePlatform) throws Exception {
        // GiantBomb doesn't need to filter by platform, the apiDetailUrl points to an exact entry.
        LOG.info("Getting details for apiDetailUrl={}...", apiDetailUrl);
        final String rawBody = client.fetchDetails(apiDetailUrl);
        LOG.debug("rawBody={}", rawBody);

        final JsonNode root = objectMapper.readTree(rawBody);
        final int statusCode = root.get("status_code").asInt();
        if (statusCode == 101) {
            LOG.info("Not found.");
            return Optional.empty();
        }
        if (statusCode != 1) {
            throw new RuntimeException("Error executing request: getDetails. apiDetailUrl=" + apiDetailUrl);
        }

        final JsonNode resultNode = root.get("results");
        final GameDetailedInfo info = translateGetDetailsResult(resultNode, gamePlatform);
        LOG.info("Found: {}", info);
        return Optional.of(info);
    }

    private GameDetailedInfo translateGetDetailsResult(JsonNode node, GamePlatform gamePlatform) {
        final String name = node.get("name").asText();
        final Optional<String> description = Optional.of(node.get("deck").asText());
        final Optional<LocalDate> releaseDate = translateDate(node.get("original_release_date").asText());

        // FIXME: Collect review scores in another API call.
        final double criticScore = 0.0;
        final double userScore = 0.0;

        final List<String> genres = extractField(node.get("genres"), "name");
        final List<String> publishers = extractField(node.get("publishers"), "name");
        final List<String> developers = extractField(node.get("developers"), "name");
        final String url = node.get("site_detail_url").asText();
        final String thumbnailUrl = extractThumbnailUrl(node);

        return new GameDetailedInfo(
            name,
            description,
            gamePlatform,
            releaseDate,
            criticScore,
            userScore,
            genres,
            publishers,
            developers,
            url,
            thumbnailUrl
        );
    }

    private String extractThumbnailUrl(JsonNode root) {
        return extractImageUrl(root, "thumb_url");
    }

    private String extractImageUrl(JsonNode root, String imageName) {
        return root.get("image").get(imageName).asText();
    }

    private List<String> extractField(JsonNode root, String fieldName) {
        final List<String> genres = new ArrayList<>();
        final Iterator<JsonNode> iterator = root.getElements();
        while (iterator.hasNext()) {
            final JsonNode node = iterator.next();
            genres.add(node.get(fieldName).asText());
        }
        return genres;
    }
}

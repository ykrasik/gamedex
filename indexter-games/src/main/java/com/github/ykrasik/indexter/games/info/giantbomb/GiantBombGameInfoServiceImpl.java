package com.github.ykrasik.indexter.games.info.giantbomb;

import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.ImageData;
import com.github.ykrasik.indexter.games.datamodel.info.giantbomb.GiantBombGameInfo;
import com.github.ykrasik.indexter.games.datamodel.info.giantbomb.GiantBombSearchResult;
import com.github.ykrasik.indexter.games.info.giantbomb.client.GiantBombGameInfoClient;
import com.github.ykrasik.indexter.games.info.giantbomb.config.GiantBombProperties;
import com.github.ykrasik.indexter.util.UrlUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.github.ykrasik.indexter.util.JsonUtils.*;

/**
 * @author Yevgeny Krasik
 */
public class GiantBombGameInfoServiceImpl implements GiantBombGameInfoService {
    private static final Logger LOG = LoggerFactory.getLogger(GiantBombGameInfoServiceImpl.class);

    private final GiantBombGameInfoClient client;
    private final GiantBombProperties properties;
    private final ObjectMapper mapper;

    public GiantBombGameInfoServiceImpl(GiantBombGameInfoClient client, GiantBombProperties properties, ObjectMapper mapper) {
        this.client = Objects.requireNonNull(client);
        this.properties = Objects.requireNonNull(properties);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public List<GiantBombSearchResult> searchGames(String name, GamePlatform platform) throws Exception {
        LOG.info("Searching for name='{}', platform={}...", name, platform);
        final int platformId = properties.getPlatformId(platform);
        final String reply = client.searchGames(name, platformId);
        LOG.debug("reply = {}", reply);

        final JsonNode root = mapper.readTree(reply);

        final int statusCode = getStatusCode(root);
        if (statusCode != 1) {
            throw new IndexterException("SearchGames: Invalid status code. name=%s, platform=%s, statusCode=%d", name, platform, statusCode);
        }

        final JsonNode results = getResults(root);
        final List<GiantBombSearchResult> searchResults = mapList(results, this::translateSearchResult);
        LOG.info("Found {} results.", searchResults.size());
        return searchResults;
    }

    private GiantBombSearchResult translateSearchResult(JsonNode node) {
        final String name = getName(node);
        final Optional<LocalDate> releaseDate = getReleaseDate(node);
        final String apiDetailUrl = getApiDetailUrl(node);
//        final Optional<String> tinyImageUrl = getTinyImageUrl(node);

        return new GiantBombSearchResult(name, releaseDate, apiDetailUrl);
    }

    @Override
    public Optional<GiantBombGameInfo> getGameInfo(String apiDetailUrl) throws Exception {
        LOG.info("Getting info for apiDetailUrl={}...", apiDetailUrl);
        final String reply = client.fetchGameInfo(apiDetailUrl);
        LOG.debug("reply = {}", reply);

        final JsonNode root = mapper.readTree(reply);

        final int statusCode = getStatusCode(root);
        if (statusCode != 1) {
            if (statusCode == 101) {
                LOG.info("Not found.");
                return Optional.empty();
            } else {
                throw new IndexterException("GetGameInfo: Invalid status code. apiDetailUrl=%s, statusCode=%d", apiDetailUrl, statusCode);
            }
        }

        final JsonNode results = getResults(root);
        final GiantBombGameInfo gameInfo = translateGame(results, apiDetailUrl);
        LOG.info("Found: {}", gameInfo);
        return Optional.of(gameInfo);
    }

    private GiantBombGameInfo translateGame(JsonNode node, String apiDetailUrl) throws Exception {
        final String name = getName(node);
        final Optional<String> description = getDescription(node);
        final Optional<LocalDate> releaseDate = getReleaseDate(node);
        final Optional<ImageData> thumbnail = getThumbnail(node);
        final Optional<ImageData> poster = getPoster(node);
        final List<String> genres = getGenres(node);

        return new GiantBombGameInfo(
            name, description, releaseDate, apiDetailUrl, thumbnail, poster, genres
        );
    }

    private int getStatusCode(JsonNode root) {
        return getMandatoryInt(root, "status_code");
    }

    private JsonNode getResults(JsonNode root) {
        return getMandatoryField(root, "results");
    }

    private String getName(JsonNode node) {
        return getMandatoryString(node, "name");
    }

    private Optional<String> getDescription(JsonNode node) {
        return getString(node, "deck");
    }

    private String getApiDetailUrl(JsonNode node) {
        return getMandatoryString(node, "api_detail_url");
    }

    private Optional<LocalDate> getReleaseDate(JsonNode node) {
        final Optional<String> originalReleaseDate = getString(node, "original_release_date");
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

//    private Optional<String> getTinyImageUrl(JsonNode node) {
//        return getImageUrl(node, "tiny_url");
//    }

    private Optional<ImageData> getThumbnail(JsonNode node) throws IOException {
        return getImageData(node, "thumb_url");
    }

    private Optional<ImageData> getPoster(JsonNode node) throws IOException {
        return getImageData(node, "super_url");
    }

    private Optional<ImageData> getImageData(JsonNode node, String imageName) throws IOException {
        return getRawImageData(node, imageName).map(ImageData::of);
    }

    private Optional<byte[]> getRawImageData(JsonNode node, String imageName) throws IOException {
        return UrlUtils.fetchOptionalUrl(getImageUrl(node, imageName));
    }

    private Optional<String> getImageUrl(JsonNode node, String imageName) {
        final Optional<JsonNode> image = getField(node, "image");
        return image.flatMap(imageNode -> getString(imageNode, imageName));
    }

    private List<String> getGenres(JsonNode node) {
        return getListOfStrings(node.get("genres"), "name");
    }

    private List<String> getListOfStrings(JsonNode root, String fieldName) {
        return flatMapList(root, node -> getString(node, fieldName));
    }
}
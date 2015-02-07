package com.github.ykrasik.indexter.games.info.giantbomb;

import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.ImageData;
import com.github.ykrasik.indexter.games.datamodel.info.giantbomb.GiantBombGameInfo;
import com.github.ykrasik.indexter.games.datamodel.info.giantbomb.GiantBombSearchResult;
import com.github.ykrasik.indexter.games.info.giantbomb.client.GiantBombGameInfoClient;
import com.github.ykrasik.indexter.games.info.giantbomb.config.GiantBombProperties;
import com.github.ykrasik.indexter.util.UrlUtils;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;

import static com.github.ykrasik.indexter.util.JsonUtils.*;

/**
 * @author Yevgeny Krasik
 */
@Slf4j
@RequiredArgsConstructor
public class GiantBombGameInfoServiceImpl implements GiantBombGameInfoService {
    @NonNull private final GiantBombGameInfoClient client;
    @NonNull private final GiantBombProperties properties;
    @NonNull private final ObjectMapper mapper;

    @Override
    public List<GiantBombSearchResult> searchGames(String name, GamePlatform platform) throws Exception {
        log.info("Searching for name='{}', platform={}...", name, platform);
        final int platformId = properties.getPlatformId(platform);
        final String reply = client.searchGames(name, platformId);
        log.debug("reply = {}", reply);

        final JsonNode root = mapper.readTree(reply);

        final int statusCode = getStatusCode(root);
        if (statusCode != 1) {
            throw new IndexterException("SearchGames: Invalid status code. name=%s, platform=%s, statusCode=%d", name, platform, statusCode);
        }

        final JsonNode results = getResults(root);
        final List<GiantBombSearchResult> searchResults = mapList(results, this::translateSearchResult);
        log.info("Found {} results.", searchResults.size());
        return searchResults;
    }

    @Override
    public Optional<GiantBombGameInfo> getGameInfo(GiantBombSearchResult searchResult) throws Exception {
        log.info("Getting info for searchResult={}...", searchResult);
        final String detailUrl = searchResult.getDetailUrl();
        final String reply = client.fetchDetails(detailUrl);
        log.debug("reply = {}", reply);

        final JsonNode root = mapper.readTree(reply);

        final int statusCode = getStatusCode(root);
        if (statusCode != 1) {
            if (statusCode == 101) {
                log.info("Not found.");
                return Optional.empty();
            } else {
                throw new IndexterException("GetGameInfo: Invalid status code. detailUrl=%s, statusCode=%d", detailUrl, statusCode);
            }
        }

        final JsonNode results = getResults(root);
        final GiantBombGameInfo gameInfo = translateGame(results, detailUrl);
        log.info("Found: {}", gameInfo);
        return Optional.of(gameInfo);
    }

    private GiantBombSearchResult translateSearchResult(JsonNode node) {
        final String name = getName(node);
        final Optional<LocalDate> releaseDate = getReleaseDate(node);
        final String apiDetailUrl = getApiDetailUrl(node);
//        final Optional<String> tinyImageUrl = getTinyImageUrl(node);

        return new GiantBombSearchResult(name, releaseDate, apiDetailUrl);
    }

    private GiantBombGameInfo translateGame(JsonNode node, String detailUrl) throws Exception {
        final String name = getName(node);
        final Optional<String> description = getDescription(node);
        final Optional<LocalDate> releaseDate = getReleaseDate(node);
        final Optional<ImageData> thumbnail = getThumbnail(node);
        final Optional<ImageData> poster = getPoster(node);
        final List<String> genres = getGenres(node);

        return new GiantBombGameInfo(
            name, description, releaseDate, detailUrl, thumbnail, poster, genres
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
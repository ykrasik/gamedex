package com.github.ykrasik.gamedex.provider.giantbomb;

import com.github.ykrasik.gamedex.common.exception.GameDexException;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.ImageData;
import com.github.ykrasik.gamedex.datamodel.info.GameInfo;
import com.github.ykrasik.gamedex.datamodel.info.SearchResult;
import com.github.ykrasik.gamedex.provider.GameInfoProviderType;
import com.github.ykrasik.gamedex.provider.GameInfoProvider;
import com.github.ykrasik.gamedex.provider.giantbomb.client.GiantBombGameInfoClient;
import com.github.ykrasik.gamedex.provider.giantbomb.config.GiantBombProperties;
import com.github.ykrasik.gamedex.common.util.UrlUtils;
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

import static com.github.ykrasik.gamedex.provider.giantbomb.GiantBombApi.*;
import static com.github.ykrasik.gamedex.provider.util.JsonUtils.*;

/**
 * @author Yevgeny Krasik
 */
@Slf4j
@RequiredArgsConstructor
public class GiantBombGameInfoService implements GameInfoProvider {
    @NonNull private final GiantBombGameInfoClient client;
    @NonNull private final GiantBombProperties properties;
    @NonNull private final ObjectMapper mapper;

    @Override
    public GameInfoProviderType getProviderType() {
        return GameInfoProviderType.GIANT_BOMB;
    }

    @Override
    public List<SearchResult> searchGames(String name, GamePlatform platform) throws Exception {
        log.info("Searching for name='{}', platform={}...", name, platform);
        final int platformId = properties.getPlatformId(platform);
        final String reply = client.searchGames(name, platformId);
        final JsonNode root = mapper.readTree(reply);

        final int statusCode = getStatusCode(root);
        if (statusCode != 1) {
            throw new GameDexException("SearchGames: Invalid status code. name=%s, platform=%s, statusCode=%d", name, platform, statusCode);
        }

        final JsonNode results = getResults(root);
        final List<SearchResult> searchResults = mapList(results, this::translateSearchResult);
        log.info("Found {} results.", searchResults.size());
        return searchResults;
    }

    private SearchResult translateSearchResult(JsonNode node) {
        return SearchResult.builder()
            .detailUrl(getDetailUrl(node))
            .name(getName(node))
            .releaseDate(getReleaseDate(node))
            .score(Optional.<Double>empty())
            .build();
    }

    @Override
    public Optional<GameInfo> getGameInfo(SearchResult searchResult) throws Exception {
        log.info("Getting info for searchResult={}...", searchResult);
        final String detailUrl = searchResult.getDetailUrl();
        final String reply = client.fetchDetails(detailUrl);
        final JsonNode root = mapper.readTree(reply);

        final int statusCode = getStatusCode(root);
        if (statusCode != 1) {
            if (statusCode == 101) {
                log.info("Not found.");
                return Optional.empty();
            } else {
                throw new GameDexException("GetGameInfo: Invalid status code. detailUrl=%s, statusCode=%d", detailUrl, statusCode);
            }
        }

        final JsonNode results = getResults(root);
        final GameInfo gameInfo = translateGame(results, detailUrl);
        log.info("Found: {}", gameInfo);
        return Optional.of(gameInfo);
    }

    private GameInfo translateGame(JsonNode node, String detailUrl) throws Exception {
        return GameInfo.builder()
            .detailUrl(detailUrl)
            .name(getName(node))
            .description(getDescription(node))
            .releaseDate(getReleaseDate(node))
            .criticScore(Optional.empty())
            .userScore(Optional.empty())
            .url(getUrl(node))
            .thumbnail(getThumbnail(node))
            .poster(getPoster(node))
            .genres(getGenres(node))
            .build();
    }

    private int getStatusCode(JsonNode root) {
        return getMandatoryInt(root, STATUS_CODE);
    }

    private JsonNode getResults(JsonNode root) {
        return getMandatoryField(root, RESULTS);
    }

    private String getDetailUrl(JsonNode node) {
        return getMandatoryString(node, DETAIL_URL);
    }

    private String getName(JsonNode node) {
        return getMandatoryString(node, NAME);
    }

    private Optional<String> getDescription(JsonNode node) {
        return getString(node, DESCRIPTION);
    }

    private Optional<LocalDate> getReleaseDate(JsonNode node) {
        return getString(node, RELEASE_DATE).flatMap(this::translateDate);
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

    private String getUrl(JsonNode node) {
        return getMandatoryString(node, URL);
    }

//    private Optional<String> getTinyImageUrl(JsonNode node) {
//        return getImageUrl(node, "tiny_url");
//    }

    private Optional<ImageData> getThumbnail(JsonNode node) throws IOException {
        return getImageData(node, IMAGE_THUMBNAIL);
    }

    private Optional<ImageData> getPoster(JsonNode node) throws IOException {
        return getImageData(node, IMAGE_POSTER);
    }

    private Optional<ImageData> getImageData(JsonNode node, String imageName) throws IOException {
        return getRawImageData(node, imageName).map(ImageData::of);
    }

    private Optional<byte[]> getRawImageData(JsonNode node, String imageName) throws IOException {
        final Optional<String> imageUrl = getImageUrl(node, imageName);
        return UrlUtils.fetchOptionalUrl(imageUrl);
    }

    private Optional<String> getImageUrl(JsonNode node, String imageName) {
        final Optional<JsonNode> image = getField(node, IMAGE);
        return image.flatMap(imageNode -> getString(imageNode, imageName));
    }

    private List<String> getGenres(JsonNode node) {
        return getListOfStrings(node.get(GENRES), NAME);
    }

    private List<String> getListOfStrings(JsonNode root, String fieldName) {
        return flatMapList(root, node -> getString(node, fieldName));
    }
}
package com.github.ykrasik.gamedex.provider.metacritic;

import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.ImageData;
import com.github.ykrasik.gamedex.datamodel.info.GameInfo;
import com.github.ykrasik.gamedex.datamodel.info.SearchResult;
import com.github.ykrasik.gamedex.provider.GameInfoProviderType;
import com.github.ykrasik.gamedex.provider.GameInfoProvider;
import com.github.ykrasik.gamedex.provider.metacritic.client.MetacriticGameInfoClient;
import com.github.ykrasik.gamedex.provider.metacritic.config.MetacriticProperties;
import com.github.ykrasik.gamedex.common.optional.Optionals;
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

import static com.github.ykrasik.gamedex.provider.util.JsonUtils.*;
import static com.github.ykrasik.gamedex.provider.metacritic.MetacriticApi.*;

/**
 * @author Yevgeny Krasik
 */
@Slf4j
@RequiredArgsConstructor
public class MetacriticGameInfoService implements GameInfoProvider {
    @NonNull private final MetacriticGameInfoClient client;
    @NonNull private final MetacriticProperties properties;
    @NonNull private final ObjectMapper mapper;

    @Override
    public GameInfoProviderType getProviderType() {
        return GameInfoProviderType.METACRITIC;
    }

    @Override
    public List<SearchResult> searchGames(String name, GamePlatform platform) throws Exception {
        log.info("Searching for name='{}', platform={}...", name, platform);
        final int platformId = properties.getPlatformId(platform);
        final String reply = client.searchGames(name, platformId);
        final JsonNode root = mapper.readTree(reply);

        final JsonNode results = getSearchResults(root);
        final List<SearchResult> searchResults = mapList(results, this::translateSearchResult);
        log.info("Found {} results.", searchResults.size());
        return searchResults;
    }

    private SearchResult translateSearchResult(JsonNode node) {
        return SearchResult.builder()
            .detailUrl(getUrl(node))
            .name(getName(node))
            .releaseDate(getReleaseDate(node))
            .score(getCriticScore(node))
            .build();
    }

    @Override
    public Optional<GameInfo> getGameInfo(SearchResult searchResult) throws Exception {
        log.info("Getting info for searchResult={}...", searchResult);
        final String detailUrl = searchResult.getDetailUrl();
        final String reply = client.fetchDetails(detailUrl);
        final JsonNode root = mapper.readTree(reply);

        final JsonNode result = getMandatoryField(root, FETCH_DETAILS_RESULT);
        if (result.isBoolean() && !result.asBoolean()) {
            log.info("Not found.");
            return Optional.empty();
        }

        final GameInfo gameInfo = translateGame(result, detailUrl);
        log.info("Found: {}", gameInfo);
        return Optional.of(gameInfo);
    }

    public GameInfo translateGame(JsonNode node, String detailUrl) throws IOException {
        return GameInfo.builder()
            .detailUrl(detailUrl)
            .name(getName(node))
            .description(getDescription(node))
            .releaseDate(getReleaseDate(node))
            .criticScore(getCriticScore(node))
            .userScore(getUserScore(node))
            .url(detailUrl)
            .thumbnail(getThumbnail(node))
            .poster(Optional.<ImageData>empty())
            .genres(getGenre(node))
            .build();
    }

    private JsonNode getSearchResults(JsonNode root) {
        return getMandatoryField(root, SEARCH_RESULTS);
    }

    private String getUrl(JsonNode node) {
        return getMandatoryString(node, URL);
    }

    private String getName(JsonNode node) {
        return getMandatoryString(node, NAME);
    }

    private Optional<LocalDate> getReleaseDate(JsonNode node) {
        return getString(node, RELEASE_DATE).flatMap(this::translateDate);
    }

    private Optional<String> getDescription(JsonNode node) {
        return getString(node, DESCRIPTION)
            .map(this::removeLastExpand)
            .filter(desc -> !desc.isEmpty());
    }

    private Optional<Double> getCriticScore(JsonNode node) {
        return getDouble(node, CRITIC_SCORE).filter(score -> score != 0.0);
    }

    private Optional<Double> getUserScore(JsonNode node) {
        // userScore is on a scale of 1-10, while for some reason criticScore is on a scale of 1-100.
        return getDouble(node, USER_SCORE).map(score -> score * 10).filter(score -> score != 0.0);
    }

    private List<String> getGenre(JsonNode node) {
        return Optionals.toList(getString(node, GENRE));
    }

    private Optional<LocalDate> translateDate(String raw) {
        try {
            return Optional.of(LocalDate.parse(raw));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    private Optional<ImageData> getThumbnail(JsonNode node) throws IOException {
        return getThumbnailData(node).map(ImageData::of);
    }

    private Optional<byte[]> getThumbnailData(JsonNode node) throws IOException {
        return UrlUtils.fetchOptionalUrl(getThumbnailUrl(node));
    }

    private Optional<String> getThumbnailUrl(JsonNode node) {
        return getString(node, IMAGE_THUMBNAIL).filter(url -> !NO_THUMBNAIL_URL.equals(url));
    }

    private String removeLastExpand(String str) {
        // Remove the last "expand" word that Metacritic API adds to the description
        final int index = str.lastIndexOf("… Expand");
        return index != -1 ? str.substring(0, index).trim() : str;
    }
}

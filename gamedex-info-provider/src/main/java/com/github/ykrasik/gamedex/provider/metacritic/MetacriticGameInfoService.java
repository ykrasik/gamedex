package com.github.ykrasik.gamedex.provider.metacritic;

import com.github.ykrasik.gamedex.common.util.UrlUtils;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.ImageData;
import com.github.ykrasik.gamedex.datamodel.provider.GameInfo;
import com.github.ykrasik.gamedex.datamodel.provider.SearchResult;
import com.github.ykrasik.gamedex.provider.GameInfoProvider;
import com.github.ykrasik.gamedex.provider.GameInfoProviderType;
import com.github.ykrasik.gamedex.provider.metacritic.client.MetacriticGameInfoClient;
import com.github.ykrasik.gamedex.provider.metacritic.config.MetacriticProperties;
import com.github.ykrasik.opt.Opt;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import static com.github.ykrasik.gamedex.provider.metacritic.MetacriticApi.*;
import static com.github.ykrasik.gamedex.provider.util.JsonUtils.*;

/**
 * @author Yevgeny Krasik
 */
@Slf4j
@RequiredArgsConstructor
public class MetacriticGameInfoService implements GameInfoProvider {
    private static final LocalDate MIN_DATE = LocalDate.of(1980, 1, 1);

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
    public Opt<GameInfo> getGameInfo(SearchResult searchResult) throws Exception {
        log.info("Getting info for searchResult={}...", searchResult);
        final String detailUrl = searchResult.getDetailUrl();
        final String reply = client.fetchDetails(detailUrl);
        final JsonNode root = mapper.readTree(reply);

        final JsonNode result = getMandatoryField(root, FETCH_DETAILS_RESULT);
        if (result.isBoolean() && !result.asBoolean()) {
            log.info("Not found.");
            return Opt.absent();
        }

        final GameInfo gameInfo = translateGame(result, detailUrl);
        log.info("Found: {}", gameInfo);
        return Opt.of(gameInfo);
    }

    public GameInfo translateGame(JsonNode node, String detailUrl) throws IOException {
        return GameInfo.builder()
            .detailUrl(detailUrl)
            .name(getName(node))
            .description(getDescription(node))
            .releaseDate(getReleaseDate(node))
            .criticScore(getCriticScore(node))
            .userScore(getUserScore(node))
            .thumbnail(getThumbnail(node))
            .poster(Opt.absent())
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

    private Opt<LocalDate> getReleaseDate(JsonNode node) {
        return getString(node, RELEASE_DATE)
            .flatMap(this::translateDate)
            .filter(date -> date.isAfter(MIN_DATE));
    }

    private Opt<String> getDescription(JsonNode node) {
        return getString(node, DESCRIPTION)
            .map(this::removeLastExpand)
            .filter(desc -> !desc.isEmpty());
    }

    private Opt<Double> getCriticScore(JsonNode node) {
        return getDouble(node, CRITIC_SCORE).filter(score -> score != 0.0);
    }

    private Opt<Double> getUserScore(JsonNode node) {
        // userScore is on a scale of 1-10, while for some reason criticScore is on a scale of 1-100.
        return getDouble(node, USER_SCORE).map(score -> score * 10).filter(score -> score != 0.0);
    }

    private List<String> getGenre(JsonNode node) {
        return getString(node, GENRE).toList();
    }

    private Opt<LocalDate> translateDate(String raw) {
        try {
            return Opt.of(LocalDate.parse(raw));
        } catch (DateTimeParseException e) {
            return Opt.absent();
        }
    }

    private Opt<ImageData> getThumbnail(JsonNode node) throws IOException {
        return getThumbnailData(node).map(ImageData::of);
    }

    private Opt<byte[]> getThumbnailData(JsonNode node) throws IOException {
        return UrlUtils.fetchOptionalUrl(getThumbnailUrl(node));
    }

    private Opt<String> getThumbnailUrl(JsonNode node) {
        return getString(node, IMAGE_THUMBNAIL).filter(url -> !NO_THUMBNAIL_URL.equals(url));
    }

    private String removeLastExpand(String str) {
        // Remove the last "expand" word that Metacritic API adds to the description
        final int index = str.lastIndexOf("… Expand");
        return index != -1 ? str.substring(0, index).trim() : str;
    }
}

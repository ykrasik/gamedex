package com.github.ykrasik.indexter.games.info.metacritic;

import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.ImageData;
import com.github.ykrasik.indexter.games.datamodel.info.metacritic.MetacriticGameInfo;
import com.github.ykrasik.indexter.games.datamodel.info.metacritic.MetacriticSearchResult;
import com.github.ykrasik.indexter.games.info.metacritic.client.MetacriticGameInfoClient;
import com.github.ykrasik.indexter.games.info.metacritic.config.MetacriticProperties;
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
public class MetacriticGameInfoServiceImpl implements MetacriticGameInfoService {
    private static final Logger LOG = LoggerFactory.getLogger(MetacriticGameInfoServiceImpl.class);
    private static final String NO_THUMBNAIL_URL = "http://static.metacritic.com/images/products/games/98w-game.jpg";

    private final MetacriticGameInfoClient client;
    private final MetacriticProperties properties;
    private final ObjectMapper mapper;

    public MetacriticGameInfoServiceImpl(MetacriticGameInfoClient client,
                                         MetacriticProperties properties,
                                         ObjectMapper mapper) {
        this.client = Objects.requireNonNull(client);
        this.properties = Objects.requireNonNull(properties);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    public List<MetacriticSearchResult> searchGames(String name, GamePlatform platform) throws Exception {
        LOG.info("Searching for name={}, platform={}...", name, platform);
        final int platformId = properties.getPlatformId(platform);
        final String reply = client.searchGames(name, platformId);
        LOG.debug("reply = {}", reply);

        final JsonNode root = mapper.readTree(reply);

        final JsonNode results = getResults(root);
        final List<MetacriticSearchResult> searchResults = mapList(results, this::translateSearchResult);
        LOG.info("Found {} results.", searchResults.size());
        return searchResults;
    }

    private MetacriticSearchResult translateSearchResult(JsonNode node) {
        final String name = getName(node);
        final Optional<LocalDate> releaseDate = getReleaseDate(node);
        final Optional<Double> score = getScore(node);

        return new MetacriticSearchResult(name, releaseDate, score);
    }

    @Override
    public Optional<MetacriticGameInfo> getGameInfo(String moreDetailsId, GamePlatform platform) throws Exception {
        LOG.info("Getting info for name={}, platform={}...", moreDetailsId, platform);
        final int platformId = properties.getPlatformId(platform);
        final String reply = client.fetchDetails(moreDetailsId, platformId);
        LOG.debug("reply = {}", reply);

        final JsonNode root = mapper.readTree(reply);

        final JsonNode result = getMandatoryField(root, "result");
        if (result.isBoolean() && !result.asBoolean()) {
            LOG.info("Not found.");
            return Optional.empty();
        }

        final MetacriticGameInfo gameInfo = translateGame(result);
        LOG.info("Found: {}", gameInfo);
        return Optional.of(gameInfo);
    }

    public MetacriticGameInfo translateGame(JsonNode node) throws IOException {
        final String name = getName(node);
        final Optional<String> description = getDescription(node);
        final Optional<LocalDate> releaseDate = getReleaseDate(node);
        final Optional<Double> criticScore = getCriticScore(node);
        final Optional<Double> userScore = getUserScore(node);
        final String url = getUrl(node);
        final Optional<ImageData> thumbnailData = getThumbnail(node);
        final Optional<String> genre = getGenre(node);

        return new MetacriticGameInfo(
            name, description, releaseDate, criticScore, userScore, url, thumbnailData, genre
        );
    }

    private JsonNode getResults(JsonNode root) {
        return getMandatoryField(root, "results");
    }

    private String getName(JsonNode node) {
        return getMandatoryString(node, "name");
    }

    private Optional<LocalDate> getReleaseDate(JsonNode node) {
        return getString(node, "rlsdate").flatMap(this::translateDate);
    }

    private Optional<Double> getScore(JsonNode node) {
        return getDouble(node, "score");
    }

    private Optional<String> getDescription(JsonNode node) {
        return getString(node, "summary")
            .map(this::removeLastExpand)
            .filter(desc -> !desc.isEmpty());
    }

    private Optional<Double> getCriticScore(JsonNode node) {
        return getDouble(node, "score").filter(score -> score != 0.0);
    }

    private Optional<Double> getUserScore(JsonNode node) {
        // userScore is on a scale of 1-10, while for some reason criticScore is on a scale of 1-100.
        return getDouble(node, "userscore").map(score -> score * 10).filter(score -> score != 0.0);
    }

    private String getUrl(JsonNode node) {
        return getMandatoryString(node, "url");
    }

    private Optional<String> getGenre(JsonNode node) {
        return getString(node, "genre");
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
        return getString(node, "thumbnail").filter(url -> !NO_THUMBNAIL_URL.equals(url));
    }

    private String removeLastExpand(String str) {
        // Remove the last "expand" word that Metacritic API adds to the description
        final int index = str.lastIndexOf("… Expand");
        return index != -1 ? str.substring(0, index).trim() : str;
    }
}

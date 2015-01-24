package com.github.ykrasik.indexter.games.info.metacritic;

import com.github.ykrasik.indexter.games.datamodel.Game;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.AbstractGameInfoService;
import com.github.ykrasik.indexter.games.info.GameRawBriefInfo;
import com.github.ykrasik.indexter.games.info.metacritic.client.MetacriticGameInfoClient;
import com.github.ykrasik.indexter.games.info.metacritic.config.MetacriticProperties;
import com.github.ykrasik.indexter.util.Optionals;
import com.github.ykrasik.indexter.util.UrlUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

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
public class MetacriticGameInfoService extends AbstractGameInfoService {
    private  static final String NO_THUMBNAIL_URL = "http://static.metacritic.com/images/products/games/98w-game.jpg";

    private final MetacriticGameInfoClient client;
    private final MetacriticProperties properties;
    private final ObjectMapper mapper;

    public MetacriticGameInfoService(MetacriticGameInfoClient client, MetacriticProperties properties, ObjectMapper mapper) {
        this.client = Objects.requireNonNull(client);
        this.properties = Objects.requireNonNull(properties);
        this.mapper = Objects.requireNonNull(mapper);
    }

    @Override
    protected void doStart() throws Exception {

    }

    @Override
    protected void doStop() throws Exception {

    }

    @Override
    public List<GameRawBriefInfo> searchGames(String name, GamePlatform platform) throws Exception {
        final String normalizedName = normalizeName(name);

        LOG.info("Searching for name={}, platform={}...", normalizedName, platform);
        final int platformId = properties.getPlatformId(platform);
        final String reply = client.searchGames(normalizedName, platformId);
        LOG.debug("reply = {}", reply);

        final JsonNode root = mapper.readTree(reply);

        final JsonNode results = getResults(root);
        final List<GameRawBriefInfo> infos = mapList(results, this::translateGameBriefInfo);
        LOG.info("Found {} results.", infos.size());
        return infos;
    }

    private GameRawBriefInfo translateGameBriefInfo(JsonNode node) {
        final String name = getName(node);
        final Optional<LocalDate> releaseDate = getReleaseDate(node);
        final Optional<Double> score = getScore(node);

        // Metacritic API doesn't provide a tinyImage on brief.
        final Optional<String> tinyImageUrl = Optional.empty();

        // Metacritic API fetches more details by name.
        final Optional<String> giantBombApiDetailUrl = Optional.empty();

        return new GameRawBriefInfo(name, releaseDate, score, tinyImageUrl, giantBombApiDetailUrl);
    }

    @Override
    public Optional<Game> getGameInfo(String moreDetailsId, GamePlatform platform) throws Exception {
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

        final Game game = translateGame(result, platform);
        LOG.info("Found: {}", game);
        return Optional.of(game);
    }

    public Game translateGame(JsonNode node, GamePlatform platform) throws IOException {
        final String name = getName(node);
        final Optional<String> description = getDescription(node);
        final Optional<LocalDate> releaseDate = getReleaseDate(node);

        final Optional<Double> criticScore = getCriticScore(node);
        final Optional<Double> userScore = getUserScore(node);

        // Only 1 genre from Metacritic API.
        final List<String> genres = getGenres(node);

        final Optional<String> giantBombApiDetailUrl = Optional.empty();

        final Optional<byte[]> thumbnailData = getThumbnailData(node);

        // No poster from Metacritic API.
        final Optional<byte[]> posterData = Optional.empty();

        return new Game(
            name, platform, description, releaseDate, criticScore, userScore, genres,
            giantBombApiDetailUrl, thumbnailData, posterData
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

    private List<String> getGenres(JsonNode node) {
        return Optionals.toList(getString(node, "genre"));
    }

    private Optional<LocalDate> translateDate(String raw) {
        try {
            return Optional.of(LocalDate.parse(raw));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
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

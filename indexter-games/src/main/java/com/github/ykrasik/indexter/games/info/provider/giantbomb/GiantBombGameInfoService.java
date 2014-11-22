package com.github.ykrasik.indexter.games.info.provider.giantbomb;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.info.GameRawBriefInfo;
import com.github.ykrasik.indexter.games.info.provider.giantbomb.client.GiantBombGameInfoClient;
import com.github.ykrasik.indexter.games.info.provider.giantbomb.config.GiantBombProperties;
import com.github.ykrasik.indexter.util.ListUtils;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.github.ykrasik.indexter.util.JsonUtils.*;

/**
 * @author Yevgeny Krasik
 */
public class GiantBombGameInfoService extends AbstractService implements GameInfoService {
    private final GiantBombGameInfoClient client;
    private final GiantBombProperties properties;
    private final ObjectMapper mapper;

    public GiantBombGameInfoService(GiantBombGameInfoClient client,
                                    GiantBombProperties properties,
                                    ObjectMapper mapper) {
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
        LOG.info("Searching for name='{}', platform={}...", name, platform);
        final int platformId = properties.getPlatformId(platform);
        final String reply = client.searchGames(name, platformId);
        LOG.debug("reply={}", reply);

        final JsonNode root = mapper.readTree(reply);

        final int statusCode = getStatusCode(root);
        if (statusCode != 1) {
            throw new IndexterException("SearchGames: Invalid status code. name=%s, platform=%s, statusCode=%d", name, platform, statusCode);
        }

        final JsonNode results = getResults(root);
        final List<GameRawBriefInfo> infos = mapList(results, this::translateGameBriefInfo);
        LOG.info("Found {} results.", infos.size());
        return infos;
    }

    private GameRawBriefInfo translateGameBriefInfo(JsonNode node) {
        final String name = getName(node);
        final Optional<LocalDate> releaseDate = getReleaseDate(node);

        // GiantBomb API doesn't provide score for brief.
        final Optional<Double> score = Optional.empty();

        final Optional<String> tinyImageUrl = getTinyImageUrl(node);

        // The GiantBomb API fetches more details by an api_detail_url field.
        final Optional<String> giantBombApiDetailUrl = Optional.of(getApiDetailUrl(node));

        return new GameRawBriefInfo(name, releaseDate, score, tinyImageUrl, giantBombApiDetailUrl);
    }

    @Override
    public Optional<GameInfo> getGameInfo(String apiDetailUrl, GamePlatform platform) throws Exception {
        // GiantBomb doesn't need to filter by platform, the apiDetailUrl points to an exact entry.
        LOG.info("Getting info for apiDetailUrl={}...", apiDetailUrl);
        final String reply = client.fetchDetails(apiDetailUrl);
        LOG.debug("reply={}", reply);

        final JsonNode root = mapper.readTree(reply);

        final int statusCode = getStatusCode(root);
        if (statusCode != 1) {
            if (statusCode == 101) {
                LOG.info("Not found.");
                return Optional.empty();
            } else {
                throw new IndexterException("GetGameInfo: Invalid status code. apiDetailUrl=%s, platform=%s, statusCode=%d", apiDetailUrl, platform, statusCode);
            }
        }

        final JsonNode results = getResults(root);
        final GameInfo info = translateGameInfo(results, platform, apiDetailUrl);
        LOG.info("Found: {}", info);
        return Optional.of(info);
    }

    private GameInfo translateGameInfo(JsonNode node, GamePlatform platform, String apiDetailUrl) throws Exception {
        final String name = getName(node);
        final Optional<String> description = getDescription(node);
        final Optional<LocalDate> releaseDate = getReleaseDate(node);

        final Optional<Double> criticScore = Optional.empty();
        final Optional<Double> userScore = Optional.empty();

        final List<String> genres = getListOfStrings(node.get("genres"), "name");
        final Optional<String> thumbnailUrl = getThumbnailUrl(node);
        final Optional<String> posterUrl = getPosterUrl(node);

        return GameInfo.from(
            name, platform, description, releaseDate, criticScore, userScore, genres, Optional.of(apiDetailUrl),
            thumbnailUrl, posterUrl
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

    private Optional<String> getThumbnailUrl(JsonNode node) {
        return getImageUrl(node, "thumb_url");
    }

    private Optional<String> getTinyImageUrl(JsonNode node) {
        return getImageUrl(node, "tiny_url");
    }

    private Optional<String> getPosterUrl(JsonNode node) {
        return getImageUrl(node, "super_url");
    }

    private Optional<String> getImageUrl(JsonNode node, String imageName) {
        final Optional<JsonNode> image = getField(node, "image");
        return image.flatMap(imageNode -> getString(imageNode, imageName));
    }

    private List<String> getListOfStrings(JsonNode root, String fieldName) {
        return flatMapList(root, node -> getString(node, fieldName));
    }

    private List<Double> getListOfDoubles(JsonNode root, String fieldName) {
        return flatMapList(root, node -> getDouble(node, fieldName));
    }

    private double getCriticScore(JsonNode node) throws Exception {
        final List<Double> criticReviewScores = getCriticReviews(node);
        return getAvgScore(criticReviewScores);
    }

    // FIXME: Return a Review class that contains more data.
    private List<Double> getCriticReviews(JsonNode node) throws Exception {
        final List<String> criticReviewUrls = getListOfStrings(node.get("reviews"), "api_detail_url");
        return ListUtils.mapToOptionThrows(criticReviewUrls, this::fetchCriticReview);
    }

    private Optional<Double> fetchCriticReview(String apiDetailUrl) throws Exception {
        LOG.info("Getting criticReview for apiDetailUrl={}...", apiDetailUrl);
        final String reply = client.fetchCriticReview(apiDetailUrl);
        LOG.debug("reply={}", reply);

        final JsonNode root = mapper.readTree(reply);

        final int statusCode = getStatusCode(root);
        if (statusCode != 1) {
            throw new IndexterException("GetCriticReview: Invalid status code. apiDetailUrl=%s, statusCode=%d", apiDetailUrl, statusCode);
        }

        final JsonNode results = getResults(root);
        return getDouble(results, "score");
    }

    private double getUserScore(JsonNode node) throws Exception {
        final List<Double> criticReviewScores = getUserReviews(node);
        return getAvgScore(criticReviewScores);
    }

    private List<Double> getUserReviews(JsonNode node) throws Exception {
        final String gameId = getMandatoryString(node, "id");
        LOG.info("Getting userReviews for gameId={}...", gameId);
        final String reply = client.fetchUserReviews(gameId);
        LOG.debug("reply={}", reply);

        final JsonNode root = mapper.readTree(reply);

        final int statusCode = getStatusCode(root);
        if (statusCode != 1) {
            throw new IndexterException("GetUserReviews: Invalid status code. gameId=%s, statusCode=%d", gameId, statusCode);
        }

        final JsonNode results = getResults(root);
        return getListOfDoubles(results, "score");
    }

    private double getAvgScore(List<Double> scores) {
        if (scores.isEmpty()) {
            return 0.0;
        }

        final double total = scores.stream().reduce(0.0, (x, y) -> x + y);
        final double avgScore = total / scores.size();

        // GiantBomb reviews are scored 0-5. Translate to a scale of 0-100.
        return avgScore / 5.0 * 100;
    }
}
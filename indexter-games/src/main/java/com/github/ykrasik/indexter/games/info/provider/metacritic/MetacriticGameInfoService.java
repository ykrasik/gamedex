package com.github.ykrasik.indexter.games.info.provider.metacritic;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.GameInfoFactory;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.info.GameRawBriefInfo;
import com.github.ykrasik.indexter.games.info.provider.metacritic.client.MetacriticGameInfoClient;
import com.github.ykrasik.indexter.games.info.provider.metacritic.config.MetacriticProperties;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * @author Yevgeny Krasik
 */
public class MetacriticGameInfoService extends AbstractService implements GameInfoService {
    private final MetacriticGameInfoClient client;
    private final MetacriticProperties properties;
    private final ObjectMapper objectMapper;

    public MetacriticGameInfoService(MetacriticGameInfoClient client,
                                     MetacriticProperties properties,
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
    public List<GameRawBriefInfo> searchGames(String name, GamePlatform gamePlatform) throws Exception {
        LOG.info("Searching for name={}, platform={}...", name, gamePlatform);
        final int platformId = properties.getPlatformId(gamePlatform);
        final String rawBody = client.searchGames(name, platformId);
        LOG.debug("rawBody={}", rawBody);

        final JsonNode root = objectMapper.readTree(rawBody);
        final List<GameRawBriefInfo> infos = new ArrayList<>();
        final Iterator<JsonNode> iterator = root.get("results").getElements();
        while (iterator.hasNext()) {
            final JsonNode node = iterator.next();
            infos.add(translateGameBriefInfo(node, gamePlatform));
        }

        LOG.info("Found {} results.", infos.size());
        return infos;
    }

    private GameRawBriefInfo translateGameBriefInfo(JsonNode node, GamePlatform gamePlatform) {
        final String name = node.get("name").asText();
        final Optional<LocalDate> releaseDate = translateDate(node.get("rlsdate").asText());
        final double score = node.get("score").asDouble();

        // Metacritic API doesn't provide a thumbnail or tinyImage on brief.
        final Optional<String> thumbnailUrl = Optional.<String>empty();
        final Optional<String> tinyImageUrl = Optional.<String>empty();

        // Metacritic API fetches more details by name.
        final String moreDetailsId = name;

        return new GameRawBriefInfo(name, gamePlatform, releaseDate, score, thumbnailUrl, tinyImageUrl, moreDetailsId);
    }

    private Optional<LocalDate> translateDate(String raw) {
        try {
            return Optional.of(LocalDate.parse(raw));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<GameInfo> getGameInfo(String moreDetailsId, GamePlatform gamePlatform) throws Exception {
        LOG.info("Getting info for name={}, platform={}...", moreDetailsId, gamePlatform);
        final int platformId = properties.getPlatformId(gamePlatform);
        final String rawBody = client.fetchDetails(moreDetailsId, platformId);
        LOG.debug("rawBody={}", rawBody);

        final JsonNode root = objectMapper.readTree(rawBody);
        final JsonNode resultNode = root.get("result");
        if (resultNode.isBoolean() && !resultNode.asBoolean()) {
            LOG.info("Not found.");
            return Optional.empty();
        } else {
            final GameInfo info = translateGameInfo(resultNode, gamePlatform);
            LOG.info("Found: {}", info);
            return Optional.of(info);
        }
    }

    private GameInfo translateGameInfo(JsonNode resultNode, GamePlatform gamePlatform) throws IOException {
        final String name = resultNode.get("name").asText();

        // Metacritic API does not provide description.
        final Optional<String> description = Optional.empty();

        final Optional<LocalDate> releaseDate = translateDate(resultNode.get("rlsdate").asText());
        final double criticScore = resultNode.get("score").asDouble();
        final double userscore = resultNode.get("userscore").asDouble();

        // Only 1 genre from Metacritic API.
        final List<String> genres = Collections.singletonList(resultNode.get("genre").asText());

        // Only 1 publisher from Metacritic API
        final List<String> publishers = Collections.singletonList(resultNode.get("publisher").asText());

        // Only 1 developer from Metacritic API
        final List<String> developers = Collections.singletonList(resultNode.get("developer").asText());

        final String url = resultNode.get("url").asText();
        final Optional<String> thumbnailUrl = Optional.of(resultNode.get("thumbnail").asText());

        return GameInfoFactory.from(
            name, description, gamePlatform, releaseDate, criticScore, userscore,
            genres, publishers, developers, url, thumbnailUrl
        );
    }
}

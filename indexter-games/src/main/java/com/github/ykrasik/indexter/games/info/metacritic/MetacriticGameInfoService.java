package com.github.ykrasik.indexter.games.info.metacritic;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.games.info.GameBriefInfo;
import com.github.ykrasik.indexter.games.info.GameDetailedInfo;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.info.Platform;
import com.github.ykrasik.indexter.games.info.metacritic.client.MetacriticGameInfoClient;
import com.github.ykrasik.indexter.games.info.metacritic.config.MetacriticProperties;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.map.ObjectMapper;

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
    public List<GameBriefInfo> searchGames(String name, Platform platform) throws Exception {
        LOG.info("Searching for name={}, platform={}...", name, platform);
        final int platformId = properties.getPlatformId(platform);
        final String rawBody = client.searchGames(name, platformId);
        LOG.debug("rawBody={}", rawBody);

        final JsonNode root = objectMapper.readTree(rawBody);
        final List<GameBriefInfo> infos = new ArrayList<>();
        final Iterator<JsonNode> iterator = root.get("results").getElements();
        while (iterator.hasNext()) {
            final JsonNode node = iterator.next();
            infos.add(translateGameBriefInfo(node, platform));
        }

        LOG.info("Found {} results.", infos.size());
        return infos;
    }

    private GameBriefInfo translateGameBriefInfo(JsonNode node, Platform platform) {
        final String name = node.get("name").asText();
        final Optional<LocalDate> releaseDate = translateDate(node.get("rlsdate").asText());
        final double score = node.get("score").asDouble();
        final Optional<String> thumbnailUrl = Optional.<String>empty();     // Metacritic API doesn't provide a thumbnail on brief.
        final String moreDetailsId = name;  // Metacritic API fetches more details by name.

        return new GameBriefInfo(
            name,
            platform,
            releaseDate,
            score,
            thumbnailUrl,
            moreDetailsId
        );
    }

    private Optional<LocalDate> translateDate(String raw) {
        try {
            final LocalDate parsed = LocalDate.parse(raw);
            return Optional.of(parsed);
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<GameDetailedInfo> getDetails(String moreDetailsId, Platform platform) throws Exception {
        LOG.info("Getting details for name={}, platform={}...", moreDetailsId, platform);
        final int platformId = properties.getPlatformId(platform);
        final String rawBody = client.fetchDetails(moreDetailsId, platformId);
        LOG.debug("rawBody={}", rawBody);

        final JsonNode root = objectMapper.readTree(rawBody);
        final JsonNode resultNode = root.get("result");
        if (resultNode.isBoolean() && !resultNode.asBoolean()) {
            LOG.info("Not found.");
            return Optional.empty();
        } else {
            final GameDetailedInfo info = translateGetDetailsResult(resultNode, platform);
            LOG.info("Found: {}", info);
            return Optional.of(info);
        }
    }

    private GameDetailedInfo translateGetDetailsResult(JsonNode resultNode, Platform platform) {
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
        final String thumbnailUrl = resultNode.get("thumbnail").asText();

        return new GameDetailedInfo(
            name,
            description,
            platform,
            releaseDate,
            criticScore,
            userscore,
            genres,
            publishers,
            developers,
            url,
            thumbnailUrl
        );
    }
}

package com.github.ykrasik.indexter.games.info.metacritic;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.games.info.GameBriefInfo;
import com.github.ykrasik.indexter.games.info.GameDetailedInfo;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.info.Platform;
import com.github.ykrasik.indexter.games.info.metacritic.client.MetacriticGameInfoClient;
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
    private final ObjectMapper objectMapper;
    private final Map<Platform, Integer> platformMap;

    public MetacriticGameInfoService(MetacriticGameInfoClient client, ObjectMapper objectMapper) {
        this.client = Objects.requireNonNull(client);
        this.objectMapper = Objects.requireNonNull(objectMapper);
        this.platformMap = new HashMap<>();
    }

    @Override
    protected void doStart() throws Exception {
        LOG.info("Fetching platforms...");
        final String rawBody = client.fetchPlatforms();
        LOG.debug("rawBody={}", rawBody);

        final JsonNode root = objectMapper.readTree(rawBody);
        platformMap.putAll(translateTypeDescriptions(root));
        LOG.info("Finished fetching platforms: {}", platformMap);
    }

    @Override
    protected void doStop() throws Exception {

    }

    private Map<Platform, Integer> translateTypeDescriptions(JsonNode root) {
        final Map<Platform, Integer> map = new HashMap<>();
        final JsonNode platformsNode = root.get("platforms");
        final Iterator<String> iterator = platformsNode.getFieldNames();
        while (iterator.hasNext()) {
            final String id = iterator.next();
            final String value = platformsNode.get(id).asText();
            final Optional<Platform> platform = translatePlatform(value);
            if (platform.isPresent()) {
                map.put(platform.get(), Integer.parseInt(id));
            }
        }
        return map;
    }

    private Optional<Platform> translatePlatform(String value) {
        switch (value) {
            case "PC": return Optional.of(Platform.PC);
            case "Xbox 360": return Optional.of(Platform.XBOX_360);
            case "Xbox One": return Optional.of(Platform.XBOX_ONE);
            case "PlayStation 3": return Optional.of(Platform.PS3);
            case "PlayStation 4": return Optional.of(Platform.PS4);
        }
        return Optional.empty();
    }

    @Override
    public List<GameBriefInfo> search(String name, Platform platform) throws Exception {
        LOG.info("Searching for name={}, platform={}...", name, platform);
        final int platformId = getPlatformId(platform);
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
        // The Metacritic API fetches more details by name.
        final String moreDetailsId = node.get("name").asText();

        return new GameBriefInfo(
            node.get("name").asText(),
            platform,
            translateDate(node.get("rlsdate").asText()),
            node.get("score").asDouble(),
            node.get("url").asText(),
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
    public Optional<GameDetailedInfo> get(String moreDetailsId, Platform platform) throws Exception {
        LOG.info("Getting details for name={}, platform={}...", moreDetailsId, platform);
        final int platformId = getPlatformId(platform);
        final String rawBody = client.fetchDetails(moreDetailsId, platformId);
        LOG.debug("rawBody={}", rawBody);

        final JsonNode root = objectMapper.readTree(rawBody);
        final JsonNode resultNode = root.get("result");
        if (resultNode.isBoolean() && !resultNode.asBoolean()) {
            LOG.info("Not found.");
            return Optional.empty();
        } else {
            final GameDetailedInfo info = translateGetResult(resultNode);
            LOG.info("Found: {}", info);
            return Optional.of(info);
        }
    }

    private GameDetailedInfo translateGetResult(JsonNode resultNode) {
        return new GameDetailedInfo(
            resultNode.get("name").asText(),
            Optional.empty(),   // No description from Metacritic API
            Platform.valueOf(resultNode.get("platform").asText()),
            translateDate(resultNode.get("rlsdate").asText()),
            resultNode.get("score").asDouble(),
            resultNode.get("userscore").asDouble(),
            Arrays.asList(resultNode.get("genre").asText()),    // Only 1 genre from Metacritic API
            resultNode.get("publisher").asText(),
            resultNode.get("developer").asText(),
            resultNode.get("url").asText(),
            resultNode.get("thumbnail").asText()
        );
    }

    private int getPlatformId(Platform platform) {
        final Integer platformId = platformMap.get(platform);
        if (platformId == null) {
            throw new RuntimeException("Unable to find id for platform: " + platform);
        }
        return platformId;
    }
}

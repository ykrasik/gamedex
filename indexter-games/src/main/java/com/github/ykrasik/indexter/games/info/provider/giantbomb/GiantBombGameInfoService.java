package com.github.ykrasik.indexter.games.info.provider.giantbomb;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.exception.IndexterException;
import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.info.GameRawBriefInfo;
import com.github.ykrasik.indexter.games.info.provider.giantbomb.client.GiantBombGameInfoClient;
import com.github.ykrasik.indexter.games.info.provider.giantbomb.config.GiantBombProperties;
import com.github.ykrasik.indexter.games.info.provider.giantbomb.translator.GiantBombTranslator;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
        LOG.info("Searching for name={}, platform={}...", name, platform);
        final int platformId = properties.getPlatformId(platform);
        final String rawBody = client.searchGames(name, platformId);
        LOG.debug("rawBody={}", rawBody);

        final GiantBombTranslator translator = new GiantBombTranslator(mapper, rawBody, platform);

        final int statusCode = translator.getStatusCode();
        if (statusCode != 1) {
            throw new IndexterException("SearchGames: Invalid status code. name=%s, platform=%s, statusCode=%d", name, platform, statusCode);
        }

        final List<GameRawBriefInfo> infos = translator.translateBriefInfos();
        LOG.info("Found {} results.", infos.size());
        return infos;
    }

    @Override
    public Optional<GameInfo> getGameInfo(String apiDetailUrl, GamePlatform platform) throws Exception {
        // GiantBomb doesn't need to filter by platform, the apiDetailUrl points to an exact entry.
        LOG.info("Getting rawInfo for apiDetailUrl={}...", apiDetailUrl);
        final String rawBody = client.fetchDetails(apiDetailUrl);
        LOG.debug("rawBody={}", rawBody);

        final GiantBombTranslator translator = new GiantBombTranslator(mapper, rawBody, platform);

        final int statusCode = translator.getStatusCode();
        if (statusCode == 101) {
            LOG.info("Not found.");
            return Optional.empty();
        }
        if (statusCode != 1) {
            throw new IndexterException("GetGameInfo: Invalid status code. apiDetailUrl=%s, platform=%s, statusCode=%d", apiDetailUrl, platform, statusCode);
        }

        final GameInfo info = translator.translateGameInfo();
        LOG.info("Found: {}", info);
        return Optional.of(info);
    }
}

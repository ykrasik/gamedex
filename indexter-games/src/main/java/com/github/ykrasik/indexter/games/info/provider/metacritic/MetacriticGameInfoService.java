package com.github.ykrasik.indexter.games.info.provider.metacritic;

import com.github.ykrasik.indexter.AbstractService;
import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.info.GameRawBriefInfo;
import com.github.ykrasik.indexter.games.info.provider.metacritic.client.MetacriticGameInfoClient;
import com.github.ykrasik.indexter.games.info.provider.metacritic.config.MetacriticProperties;
import com.github.ykrasik.indexter.games.info.provider.metacritic.translator.MetacriticTranslator;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class MetacriticGameInfoService extends AbstractService implements GameInfoService {
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
        LOG.info("Searching for name={}, platform={}...", name, platform);
        final int platformId = properties.getPlatformId(platform);
        final String rawBody = client.searchGames(name, platformId);
        LOG.debug("rawBody={}", rawBody);

        final MetacriticTranslator translator = new MetacriticTranslator(mapper, rawBody, platform);
        final List<GameRawBriefInfo> infos = translator.translateBriefInfos();
        LOG.info("Found {} results.", infos.size());
        return infos;
    }

    @Override
    public Optional<GameInfo> getGameInfo(String moreDetailsId, GamePlatform platform) throws Exception {
        LOG.info("Getting info for name={}, platform={}...", moreDetailsId, platform);
        final int platformId = properties.getPlatformId(platform);
        final String rawBody = client.fetchDetails(moreDetailsId, platformId);
        LOG.debug("rawBody={}", rawBody);

        final MetacriticTranslator translator = new MetacriticTranslator(mapper, rawBody, platform);
        final Optional<GameInfo> info = translator.translateGameInfo();
        if (info.isPresent()) {
            LOG.info("Found: {}", info.get());
        } else {
            LOG.info("Not found.");
        }
        return info;
    }
}

package com.github.ykrasik.gamedex.provider.metacritic.debug;

import com.github.ykrasik.gamedex.common.debug.DebugCommands;
import com.github.ykrasik.gamedex.datamodel.GamePlatform;
import com.github.ykrasik.gamedex.datamodel.provider.SearchResult;
import com.github.ykrasik.gamedex.provider.metacritic.MetacriticGameInfoProvider;
import com.github.ykrasik.gamedex.provider.metacritic.client.MetacriticGameInfoClient;
import com.github.ykrasik.jaci.api.*;
import com.github.ykrasik.yava.option.Opt;
import com.gs.collections.api.list.ImmutableList;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
@CommandPath("metacritic")
public class MetacriticDebugCommands implements DebugCommands {
    @NonNull private final MetacriticGameInfoProvider service;
    @NonNull private final MetacriticGameInfoClient client;
    @NonNull private final ObjectMapper objectMapper;

    private CommandOutput output;

    @Command
    public void search(@StringParam("name") String name,
                       @StringParam(value = "platform", acceptsSupplier = "platformValues", optional = true, defaultValue = "PC") String platformStr) throws Exception {
        final GamePlatform gamePlatform = GamePlatform.valueOf(platformStr);
        final ImmutableList<SearchResult> searchResults = service.search(name, gamePlatform);
        for (SearchResult searchResult : searchResults) {
            output.message(searchResult.toString());
        }
    }

    @Command
    public void fetch(@StringParam("url") String url) throws Exception {
        final SearchResult searchResult = SearchResult.builder()
            .detailUrl(url)
            .name("")
            .releaseDate(Opt.none())
            .score(Opt.none())
            .build();
        output.message(service.fetch(searchResult).toString());
    }

    @CommandPath("client")
    @Command("search")
    public void clientSearch(@StringParam("name") String name,
                             @IntParam(value = "platformId", optional = true, defaultValue = 3) int platformId) throws Exception {
        final String rawJson = client.searchGames(name, platformId);
        final Object json = objectMapper.readValue(rawJson, Object.class);
        final String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        output.message(prettyJson);
    }

    @CommandPath("client")
    @Command("get")
    public void clientGet(@StringParam("detailUrl") String detailUrl) throws Exception {
        final String rawJson = client.fetchDetails(detailUrl);
        final Object json = objectMapper.readValue(rawJson, Object.class);
        final String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        output.message(prettyJson);
    }

    private String[] platformValues() {
        final GamePlatform[] gamePlatforms = GamePlatform.values();
        final String[] values = new String[gamePlatforms.length];
        for (int i = 0; i < gamePlatforms.length; i++) {
            values[i] = gamePlatforms[i].name();
        }
        return values;
    }
}

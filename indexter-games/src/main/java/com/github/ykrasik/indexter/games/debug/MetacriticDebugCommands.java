package com.github.ykrasik.indexter.games.debug;

import com.github.ykrasik.indexter.debug.DebugCommands;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.info.SearchResult;
import com.github.ykrasik.indexter.games.info.metacritic.MetacriticGameInfoService;
import com.github.ykrasik.indexter.games.info.metacritic.client.MetacriticGameInfoClient;
import com.github.ykrasik.jerminal.api.annotation.*;
import com.github.ykrasik.jerminal.api.command.OutputPrinter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
@ShellPath("metacritic")
public class MetacriticDebugCommands implements DebugCommands {
    @NonNull private final MetacriticGameInfoService service;
    @NonNull private final MetacriticGameInfoClient client;
    @NonNull private final ObjectMapper objectMapper;

    @Command
    public void search(OutputPrinter outputPrinter,
                       @StringParam("name") String name,
                       @DynamicStringParam(value = "platform", supplier = "platformValues", optional = true, defaultValue = "PC") String platformStr) throws Exception {
        final GamePlatform gamePlatform = GamePlatform.valueOf(platformStr);
        service.searchGames(name, gamePlatform).forEach(result -> outputPrinter.println(result.toString()));
    }

    @Command
    public void get(OutputPrinter outputPrinter, @StringParam("url") String url) throws Exception {
        final SearchResult searchResult = SearchResult.builder()
            .detailUrl(url)
            .name("")
            .releaseDate(Optional.empty())
            .score(Optional.empty())
            .build();
        outputPrinter.println(service.getGameInfo(searchResult).toString());
    }

    @ShellPath("client")
    @Command("search")
    public void clientSearch(OutputPrinter outputPrinter,
                             @StringParam("name") String name,
                             @IntParam(value = "platformId", optional = true, defaultValue = 3) int platformId) throws Exception {
        final String rawJson = client.searchGames(name, platformId);
        final Object json = objectMapper.readValue(rawJson, Object.class);
        final String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        outputPrinter.println(prettyJson);
    }

    @ShellPath("client")
    @Command("get")
    public void clientGet(OutputPrinter outputPrinter, @StringParam("detailUrl") String detailUrl) throws Exception {
        final String rawJson = client.fetchDetails(detailUrl);
        final Object json = objectMapper.readValue(rawJson, Object.class);
        final String prettyJson = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(json);
        outputPrinter.println(prettyJson);
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

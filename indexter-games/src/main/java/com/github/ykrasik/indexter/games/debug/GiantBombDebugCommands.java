package com.github.ykrasik.indexter.games.debug;

import com.github.ykrasik.indexter.debug.DebugCommands;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.info.giantbomb.GiantBombSearchResult;
import com.github.ykrasik.indexter.games.info.giantbomb.GiantBombGameInfoServiceImpl;
import com.github.ykrasik.indexter.games.info.giantbomb.client.GiantBombGameInfoClient;
import com.github.ykrasik.jerminal.api.annotation.*;
import com.github.ykrasik.jerminal.api.command.OutputPrinter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.codehaus.jackson.map.ObjectMapper;

import java.time.LocalDate;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
@ShellPath("giantbomb")
public class GiantBombDebugCommands implements DebugCommands {
    @NonNull private final GiantBombGameInfoServiceImpl service;
    @NonNull private final GiantBombGameInfoClient client;
    @NonNull private final ObjectMapper objectMapper;

    @Command
    public void search(OutputPrinter outputPrinter,
                       @StringParam("name") String name,
                       @DynamicStringParam(value = "platform", supplier = "platformValues", optional = true, defaultValue = "PC") String platformStr) throws Exception {
        final GamePlatform platform = GamePlatform.valueOf(platformStr);
        service.searchGames(name, platform).forEach(result -> outputPrinter.println(String.format("%s - %s", result.getName(), result.getDetailUrl())));
    }

    @Command
    public void get(OutputPrinter outputPrinter,
                    @StringParam("url") String url) throws Exception {
        final GiantBombSearchResult searchResult = new GiantBombSearchResult("", Optional.<LocalDate>empty(), url);
        outputPrinter.println(service.getGameInfo(searchResult).toString());
    }

    @ShellPath("client")
    @Command("search")
    public void clientSearch(OutputPrinter outputPrinter,
                             @StringParam("name") String name,
                             @IntParam(value = "platformId", optional = true, defaultValue = 94) int platformId) throws Exception {
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
        final GamePlatform[] platforms = GamePlatform.values();
        final String[] values = new String[platforms.length];
        for (int i = 0; i < platforms.length; i++) {
            values[i] = platforms[i].name();
        }
        return values;
    }
}

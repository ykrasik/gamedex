package com.github.ykrasik.indexter.games.debug;

import com.github.ykrasik.indexter.debug.DebugCommands;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.provider.giantbomb.GiantBombGameInfoService;
import com.github.ykrasik.indexter.games.info.provider.giantbomb.client.GiantBombGameInfoClient;
import com.github.ykrasik.jerminal.api.annotation.*;
import com.github.ykrasik.jerminal.api.command.OutputPrinter;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
@ShellPath("giantbomb")
public class GiantBombDebugCommands implements DebugCommands {
    private final GiantBombGameInfoService service;
    private final GiantBombGameInfoClient client;
    private final ObjectMapper objectMapper;

    public GiantBombDebugCommands(GiantBombGameInfoService service,
                                  GiantBombGameInfoClient client,
                                  ObjectMapper objectMapper) {
        this.service = Objects.requireNonNull(service);
        this.client = Objects.requireNonNull(client);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Command
    public void search(OutputPrinter outputPrinter,
                       @StringParam("name") String name,
                       @DynamicStringParam(value = "platform", supplier = "platformValues", optional = true, defaultValue = "PC") String platformStr) throws Exception {
        final GamePlatform platform = GamePlatform.valueOf(platformStr);
        service.searchGames(name, platform).forEach(result -> outputPrinter.println(String.format("%s - %s", result.getName(), result.getGiantBombApiDetailUrl().orElse("None"))));
    }

    @Command
    public void get(OutputPrinter outputPrinter,
                    @StringParam("name") String name,
                    @DynamicStringParam(value = "platform", supplier = "platformValues", optional = true, defaultValue = "PC") String platformStr) throws Exception {
        final GamePlatform platform = GamePlatform.valueOf(platformStr);
        outputPrinter.println(service.getGameInfo(name, platform).toString());
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
    public void clientGet(OutputPrinter outputPrinter, @StringParam("apiDetailUrl") String apiDetailUrl) throws Exception {
        final String rawJson = client.fetchDetails(apiDetailUrl);
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

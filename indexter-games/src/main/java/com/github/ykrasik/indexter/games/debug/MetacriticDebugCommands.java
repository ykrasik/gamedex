package com.github.ykrasik.indexter.games.debug;

import com.github.ykrasik.indexter.debug.DebugCommands;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.info.metacritic.MetacriticGameInfoService;
import com.github.ykrasik.indexter.games.info.metacritic.client.MetacriticGameInfoClient;
import com.github.ykrasik.jerminal.api.annotation.*;
import com.github.ykrasik.jerminal.api.command.OutputPrinter;
import org.codehaus.jackson.map.ObjectMapper;

import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
@ShellPath("metacritic")
public class MetacriticDebugCommands implements DebugCommands {
    private final MetacriticGameInfoService service;
    private final MetacriticGameInfoClient client;
    private final ObjectMapper objectMapper;

    public MetacriticDebugCommands(MetacriticGameInfoService service,
                                   MetacriticGameInfoClient client,
                                   ObjectMapper objectMapper) {
        this.service = Objects.requireNonNull(service);
        this.client = Objects.requireNonNull(client);
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    @Command
    public void search(OutputPrinter outputPrinter,
                       @StringParam("name") String name,
                       @DynamicStringParam(value = "platform", supplier = "platformValues", optional = true, defaultValue = "PC") String platformStr) throws Exception {
        final GamePlatform gamePlatform = GamePlatform.valueOf(platformStr);
        service.searchGames(name, gamePlatform).forEach(result -> outputPrinter.println(result.toString()));
    }

    @Command
    public void get(OutputPrinter outputPrinter,
                    @StringParam("name") String name,
                    @DynamicStringParam(value = "platform", supplier = "platformValues", optional = true, defaultValue = "PC") String platformStr) throws Exception {
        final GamePlatform gamePlatform = GamePlatform.valueOf(platformStr);
        outputPrinter.println(service.getDetails(name, gamePlatform).toString());
    }

    @ShellPath("client")
    @Command("platforms")
    public void clientFetchPlatforms(OutputPrinter outputPrinter) throws Exception {
        outputPrinter.println(client.fetchPlatforms());
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
    public void clientGet(OutputPrinter outputPrinter,
                          @StringParam("name") String name,
                          @IntParam(value = "platformId", optional = true, defaultValue = 3) int platformId) throws Exception {
        final String rawJson = client.fetchDetails(name, platformId);
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

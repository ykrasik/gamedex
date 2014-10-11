package com.github.ykrasik.indexter.games.debug;

import com.github.ykrasik.indexter.games.info.Platform;
import com.github.ykrasik.indexter.games.info.metacritic.MetacriticGameInfoService;
import com.github.ykrasik.indexter.games.info.metacritic.client.MetacriticGameInfoClient;
import com.github.ykrasik.jerminal.api.annotation.Command;
import com.github.ykrasik.jerminal.api.annotation.IntParam;
import com.github.ykrasik.jerminal.api.annotation.ShellPath;
import com.github.ykrasik.jerminal.api.annotation.StringParam;
import com.github.ykrasik.jerminal.api.command.OutputPrinter;

import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
@ShellPath("metacritic")
public class MetacriticDebugCommands {
    private final MetacriticGameInfoService service;
    private final MetacriticGameInfoClient client;

    public MetacriticDebugCommands(MetacriticGameInfoService service, MetacriticGameInfoClient client) {
        this.service = Objects.requireNonNull(service);
        this.client = Objects.requireNonNull(client);
    }

    @Command
    public void search(OutputPrinter outputPrinter,
                       @StringParam("name") String name,
                       @StringParam(value = "platform", optional = true, defaultValue = "PC") String platformStr) throws Exception {
        final Platform platform = Platform.valueOf(platformStr);
        service.search(name, platform).forEach(result -> outputPrinter.println(result.toString()));
    }

    @Command
    public void get(OutputPrinter outputPrinter,
                    @StringParam("name") String name,
                    @StringParam(value = "platform", optional = true, defaultValue = "PC") String platformStr) throws Exception {
        final Platform platform = Platform.valueOf(platformStr);
        outputPrinter.println(service.get(name, platform).toString());
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
        outputPrinter.println(client.searchGames(name, platformId));
    }

    @ShellPath("client")
    @Command("get")
    public void clientGet(OutputPrinter outputPrinter,
                          @StringParam("name") String name,
                          @IntParam(value = "platformId", optional = true, defaultValue = 3) int platformId) throws Exception {
        outputPrinter.println(client.fetchDetails(name, platformId));
    }
}

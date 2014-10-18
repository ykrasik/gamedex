package com.github.ykrasik.indexter.games.debug;

import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.info.GameDetailedInfo;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.indexter.games.info.Platform;
import com.github.ykrasik.jerminal.api.annotation.Command;
import com.github.ykrasik.jerminal.api.annotation.ShellPath;
import com.github.ykrasik.jerminal.api.annotation.StringParam;
import com.github.ykrasik.jerminal.api.command.OutputPrinter;

import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
@ShellPath("data")
public class DataServiceDebugCommands {
    private final GameDataService dataService;
    private final GameInfoService service;

    public DataServiceDebugCommands(GameDataService dataService, GameInfoService service) {
        this.dataService = Objects.requireNonNull(dataService);
        this.service = Objects.requireNonNull(service);
    }

    @Command
    public void add(OutputPrinter outputPrinter,
                    @StringParam("name") String name,
                    @StringParam(value = "platform", optional = true, defaultValue = "PC") String platformStr) throws Exception {
        final Platform platform = Platform.valueOf(platformStr);
        final GameDetailedInfo info = service.getDetails(name, platform).orElseThrow(() -> new RuntimeException("No info available for: " + name));
        dataService.add(info);
    }
}

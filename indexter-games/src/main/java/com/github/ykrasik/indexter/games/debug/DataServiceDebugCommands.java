package com.github.ykrasik.indexter.games.debug;

import com.github.ykrasik.indexter.debug.DebugCommands;
import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.datamodel.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.GamePlatform;
import com.github.ykrasik.indexter.games.datamodel.LocalGameInfo;
import com.github.ykrasik.indexter.games.info.GameInfoService;
import com.github.ykrasik.jerminal.api.annotation.Command;
import com.github.ykrasik.jerminal.api.annotation.ShellPath;
import com.github.ykrasik.jerminal.api.annotation.StringParam;
import com.github.ykrasik.jerminal.api.command.OutputPrinter;

import java.nio.file.Paths;
import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
@ShellPath("data")
public class DataServiceDebugCommands implements DebugCommands {
    private final GameDataService dataService;
    private final GameInfoService infoService;

    public DataServiceDebugCommands(GameDataService dataService, GameInfoService infoService) {
        this.dataService = Objects.requireNonNull(dataService);
        this.infoService = Objects.requireNonNull(infoService);
    }

    @Command
    public void add(OutputPrinter outputPrinter,
                    @StringParam("name") String name,
                    @StringParam("path") String path,
                    @StringParam(value = "platform", optional = true, defaultValue = "PC") String platformStr) throws Exception {
        final GamePlatform gamePlatform = GamePlatform.valueOf(platformStr);
        final GameInfo gameInfo = infoService.getGameInfo(name, gamePlatform).orElseThrow(() -> new RuntimeException("No info available for: " + name));
        final LocalGameInfo info = new LocalGameInfo(Paths.get(path), gameInfo);
        dataService.add(info);
    }
}

package com.github.ykrasik.indexter.games.debug;

import com.github.ykrasik.indexter.games.info.giantbomb.GiantBombGameInfoService;
import com.github.ykrasik.jerminal.api.annotation.Command;
import com.github.ykrasik.jerminal.api.annotation.ShellPath;
import com.github.ykrasik.jerminal.api.annotation.StringParam;
import com.github.ykrasik.jerminal.api.command.OutputPrinter;

import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
@ShellPath("giantbomb")
public class GiantBombDebugCommands {
    private final GiantBombGameInfoService service;

    public GiantBombDebugCommands(GiantBombGameInfoService service) {
        this.service = Objects.requireNonNull(service);
    }

    @Command
    public void search(OutputPrinter outputPrinter, @StringParam("name") String name) throws Exception {
        outputPrinter.println(service.search(name));
    }
}

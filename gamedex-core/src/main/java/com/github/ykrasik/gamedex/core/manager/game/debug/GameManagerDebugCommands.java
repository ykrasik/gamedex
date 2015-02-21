package com.github.ykrasik.gamedex.core.manager.game.debug;

import com.github.ykrasik.gamedex.common.debug.DebugCommands;
import com.github.ykrasik.gamedex.core.manager.game.GameManager;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.gamedex.datamodel.persistence.Id;
import com.github.ykrasik.jerminal.api.annotation.Command;
import com.github.ykrasik.jerminal.api.annotation.IntParam;
import com.github.ykrasik.jerminal.api.annotation.ShellPath;
import com.github.ykrasik.jerminal.api.annotation.StringParam;
import com.github.ykrasik.jerminal.api.command.OutputPrinter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.nio.file.Paths;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
@ShellPath("game")
public class GameManagerDebugCommands implements DebugCommands {
    @NonNull private final GameManager manager;

    @Command
    public void getById(OutputPrinter outputPrinter, @IntParam("id") int id) throws Exception {
        final Game game = manager.getGameById(new Id<>(id));
        outputPrinter.println(game.toString());
    }

    @Command
    public void isGame(OutputPrinter outputPrinter, @StringParam("path") String path) throws Exception {
        outputPrinter.println(String.valueOf(manager.isGame(Paths.get(path))));
    }

    @Command
    public void all(OutputPrinter outputPrinter) throws Exception {
        final List<Game> games = manager.getAllGames();
        games.forEach(game -> outputPrinter.println(game.toString()));
    }

    @Command
    public void delete(OutputPrinter outputPrinter, @IntParam("id") int id) throws Exception {
        final Game game = manager.getGameById(new Id<>(id));
        manager.deleteGame(game);
    }
}

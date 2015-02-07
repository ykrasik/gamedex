package com.github.ykrasik.indexter.games.debug;

import com.github.ykrasik.indexter.debug.DebugCommands;
import com.github.ykrasik.indexter.games.datamodel.persistence.Game;
import com.github.ykrasik.indexter.games.manager.game.GameManager;
import com.github.ykrasik.indexter.id.Id;
import com.github.ykrasik.jerminal.api.annotation.Command;
import com.github.ykrasik.jerminal.api.annotation.IntParam;
import com.github.ykrasik.jerminal.api.annotation.ShellPath;
import com.github.ykrasik.jerminal.api.annotation.StringParam;
import com.github.ykrasik.jerminal.api.command.OutputPrinter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
@ShellPath("game")
public class GameManagerDebugCommands implements DebugCommands {
    @NonNull private final GameManager gameManager;

    @Command
    public void getById(OutputPrinter outputPrinter, @IntParam("id") int id) throws Exception {
        final Game game = gameManager.getGameById(new Id<>(id));
        outputPrinter.println(game.toString());
    }

    @Command
    public void isGame(OutputPrinter outputPrinter, @StringParam("path") String path) throws Exception {
        outputPrinter.println(String.valueOf(gameManager.isGame(Paths.get(path))));
    }

    @Command
    public void all(OutputPrinter outputPrinter) throws Exception {
        final List<Game> games = gameManager.getAllGames();
        games.forEach(game -> outputPrinter.println(game.toString()));
    }

    @Command
    public void delete(OutputPrinter outputPrinter, @IntParam("id") int id) throws Exception {
        final Game game = gameManager.getGameById(new Id<>(id));
        gameManager.deleteGame(game);
    }
}

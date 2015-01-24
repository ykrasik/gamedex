package com.github.ykrasik.indexter.games.debug;

import com.github.ykrasik.indexter.debug.DebugCommands;
import com.github.ykrasik.indexter.games.datamodel.LocalGame;
import com.github.ykrasik.indexter.games.manager.game.GameManager;
import com.github.ykrasik.indexter.id.Id;
import com.github.ykrasik.jerminal.api.annotation.Command;
import com.github.ykrasik.jerminal.api.annotation.IntParam;
import com.github.ykrasik.jerminal.api.annotation.ShellPath;
import com.github.ykrasik.jerminal.api.annotation.StringParam;
import com.github.ykrasik.jerminal.api.command.OutputPrinter;
import javafx.collections.ObservableList;

import java.nio.file.Paths;
import java.util.Objects;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
@ShellPath("game")
public class GameManagerDebugCommands implements DebugCommands {
    private final GameManager gameManager;

    public GameManagerDebugCommands(GameManager gameManager) {
        this.gameManager = Objects.requireNonNull(gameManager);
    }

    @Command
    public void getById(OutputPrinter outputPrinter, @IntParam("id") int id) throws Exception {
        final LocalGame game = gameManager.getGameById(new Id<>(id));
        outputPrinter.println(game.toString());
    }

    @Command
    public void getByPath(OutputPrinter outputPrinter, @StringParam("path") String path) throws Exception {
        final Optional<LocalGame> game = gameManager.getGameByPath(Paths.get(path));
        outputPrinter.println(game.map(Object::toString).orElse("Not found!"));
    }

    @Command
    public void all(OutputPrinter outputPrinter) throws Exception {
        final ObservableList<LocalGame> games = gameManager.getAllGames();
        games.forEach(game -> outputPrinter.println(game.toString()));
    }

    @Command
    public void delete(OutputPrinter outputPrinter, @IntParam("id") int id) throws Exception {
        final LocalGame game = gameManager.getGameById(new Id<>(id));
        gameManager.deleteGame(game);
    }
}

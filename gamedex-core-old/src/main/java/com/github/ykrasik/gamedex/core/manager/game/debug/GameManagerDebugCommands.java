package com.github.ykrasik.gamedex.core.manager.game.debug;

import com.github.ykrasik.gamedex.core.debug.DebugCommands;
import com.github.ykrasik.gamedex.core.manager.game.GameManager;
import com.github.ykrasik.gamedex.core.persistence.Id;
import com.github.ykrasik.gamedex.datamodel.Game;
import com.github.ykrasik.jaci.api.*;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.nio.file.Paths;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
@CommandPath("game")
public class GameManagerDebugCommands implements DebugCommands {
    @NonNull private final GameManager manager;

    private CommandOutput output;
    
    @Command
    public void getById(@IntParam("id") int id) throws Exception {
        final Game game = manager.getGameById(new Id<>(id));
        output.message(game.toString());
    }

    @Command
    public void isGame( @StringParam("path") String path) throws Exception {
        output.message(String.valueOf(manager.isGame(Paths.get(path))));
    }

    @Command
    public void all() throws Exception {
        final List<Game> games = manager.getAllGames();
        games.forEach(game -> output.message(game.toString()));
    }

    @Command
    public void delete(@IntParam("id") int id) throws Exception {
        final Game game = manager.getGameById(new Id<>(id));
        manager.deleteGame(game);
    }
}

package com.github.ykrasik.indexter.games.debug;

import com.github.ykrasik.indexter.games.data.GameDataService;
import com.github.ykrasik.indexter.games.info.GameDetailedInfo;
import com.github.ykrasik.jerminal.api.annotation.Command;
import com.github.ykrasik.jerminal.api.annotation.ShellPath;
import com.github.ykrasik.jerminal.api.command.OutputPrinter;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.TilePane;

import java.util.Collection;
import java.util.Objects;

/**
 * @author Yevgeny Krasik
 */
@ShellPath("ui")
public class UiDebugCommands {
    private final GameDataService dataService;
    private final TilePane gameWall;

    public UiDebugCommands(GameDataService dataService, TilePane gameWall) {
        this.dataService = Objects.requireNonNull(dataService);
        this.gameWall = Objects.requireNonNull(gameWall);
    }

    @Command
    public void redraw(OutputPrinter outputPrinter) throws Exception {
        final Collection<GameDetailedInfo> infos = dataService.getAll();
        redraw(infos);
    }

    private void redraw(Collection<GameDetailedInfo> infos) {
        gameWall.getChildren().clear();
        for (GameDetailedInfo info : infos) {
            final Image image = new Image(info.getThumbnailUrl());
            gameWall.getChildren().add(new ImageView(image));
        }
    }
}

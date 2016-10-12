package com.github.ykrasik.gamedex.core.controller.game;

import com.github.ykrasik.gamedex.core.controller.Controller;
import com.github.ykrasik.gamedex.core.manager.game.GameManager;
import com.github.ykrasik.gamedex.core.service.action.ActionService;
import com.github.ykrasik.gamedex.core.service.config.ConfigService;
import com.github.ykrasik.gamedex.core.service.image.ImageService;
import com.github.ykrasik.gamedex.core.service.screen.detail.GameDetailsScreen;
import com.github.ykrasik.gamedex.core.ui.gridview.GameWallCell;
import com.github.ykrasik.gamedex.datamodel.Game;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.input.MouseEvent;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.controlsfx.control.GridView;

/**
 * @author Yevgeny Krasik
 */
// TODO: On mouse over show quick detail view in statusBar.
@RequiredArgsConstructor
public class GameWallController implements Controller {
    @FXML private GridView<Game> gameWall;

    @NonNull private final ConfigService configService;
    @NonNull private final ImageService imageService;
    @NonNull private final ActionService actionService;

    @NonNull private final GameManager gameManager;

    @NonNull private final GameDetailsScreen gameDetailsScreen;

    private final ObjectProperty<Game> selectedGameProperty = new SimpleObjectProperty<>();

    public ReadOnlyObjectProperty<Game> selectedGameProperty() {
        return selectedGameProperty;
    }

    @FXML
    private void initialize() {
        gameWall.setCellFactory(gridView -> {
            final GameWallCell cell = new GameWallCell(configService, imageService);
            cell.getStyleClass().addAll("card", "gameTile");
            cell.setOnMouseClicked(event -> onMouseClicked(event, cell));

            final ContextMenu contextMenu = createContextMenu(cell);
            cell.setContextMenu(contextMenu);
            return cell;
        });

        // TODO: gameWall has a problem refreshing... so instead of binding, add a listener and clear the wall before setting the value.
        gameWall.itemsProperty().bind(gameManager.gamesProperty());
    }

    private ContextMenu createContextMenu(GameWallCell cell) {
        final ContextMenu contextMenu = new ContextMenu();

        final MenuItem detailsItem = new MenuItem("Details");
        detailsItem.setOnAction(e -> displayGameDetails(cell.getItem()));

//        final MenuItem showInListItem = new MenuItem("Show in List");
//        showInListItem.setOnAction(e -> selectedGameProperty.set(cell.getItem()));

        final MenuItem separator = new SeparatorMenuItem();

        final MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            final Game game = cell.getItem();
            actionService.deleteGame(game);
        });

        contextMenu.getItems().addAll(detailsItem, separator, deleteItem);
        return contextMenu;
    }

    private void onMouseClicked(MouseEvent event, GameWallCell cell) {
        final Game game = cell.getItem();
        selectedGameProperty.set(game);
        if (event.getClickCount() == 2) {
            displayGameDetails(game);
        }
    }

    private void displayGameDetails(Game game) {
        gameDetailsScreen.show(game);
    }
}

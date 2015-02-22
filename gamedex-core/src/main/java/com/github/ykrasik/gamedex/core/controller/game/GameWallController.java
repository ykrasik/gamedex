package com.github.ykrasik.gamedex.core.controller.game;

import com.github.ykrasik.gamedex.core.controller.Controller;
import com.github.ykrasik.gamedex.core.service.image.ImageService;
import com.github.ykrasik.gamedex.core.ui.detailview.GameDetailView;
import com.github.ykrasik.gamedex.core.ui.gridview.GameInfoCell;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import com.github.ykrasik.opt.Opt;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.controlsfx.control.GridView;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class GameWallController implements Controller {
    @FXML private GridView<Game> gameWall;

    @NonNull private final ImageService imageService;

    private final ObjectProperty<Game> selectedGameProperty = new SimpleObjectProperty<>();
    private final ObjectProperty<Game> deletedGameProperty = new SimpleObjectProperty<>();

    public ObjectProperty<ObservableList<Game>> itemsProperty() {
        return gameWall.itemsProperty();
    }

    public ReadOnlyObjectProperty<Game> selectedGameProperty() {
        return selectedGameProperty;
    }

    public ReadOnlyObjectProperty<Game> deletedGameProperty() {
        return deletedGameProperty;
    }

    // Called by JavaFX
    public void initialize() {
        gameWall.setCellFactory(param -> {
            final GameInfoCell cell = new GameInfoCell(imageService);
            cell.getStyleClass().addAll("card", "gameTile");
            cell.setOnMouseClicked(event -> {
                final Game game = cell.getItem();
                selectedGameProperty.set(game);
                if (event.getClickCount() == 2) {
                    displayGameDetails(game);
                }
                event.consume();
            });

            final ContextMenu contextMenu = createContextMenu(cell);
            cell.setContextMenu(contextMenu);
            return cell;
        });
    }

    private ContextMenu createContextMenu(GameInfoCell cell) {
        final ContextMenu contextMenu = new ContextMenu();

        final MenuItem detailsItem = new MenuItem("Details");
        detailsItem.setOnAction(e -> displayGameDetails(cell.getItem()));

//        final MenuItem showInListItem = new MenuItem("Show in List");
//        showInListItem.setOnAction(e -> selectedGameProperty.set(cell.getItem()));

        final MenuItem separator = new SeparatorMenuItem();

        final MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            final Game game = cell.getItem();
            // TODO: Confirmation menu. And go through actionService.
            deletedGameProperty.set(game);
        });

        contextMenu.getItems().addAll(detailsItem, separator, deleteItem);
        return contextMenu;
    }

    private void displayGameDetails(Game game) {
        // FIXME: Handle exception while editing
        final Opt<Game> editedGame = GameDetailView.create().imageService(imageService).show(game);
        if (editedGame.isPresent()) {
            // TODO: Update in db
            System.out.println(editedGame);
        }
    }
}

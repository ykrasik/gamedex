package com.github.ykrasik.gamedex.core.controller;

import com.github.ykrasik.gamedex.core.ui.gridview.GameInfoCell;
import com.github.ykrasik.gamedex.datamodel.persistence.Game;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import lombok.RequiredArgsConstructor;
import org.controlsfx.control.GridView;

/**
 * @author Yevgeny Krasik
 */
@RequiredArgsConstructor
public class GameWallController implements Controller {
    @FXML private GridView<Game> gameWall;

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
            final GameInfoCell cell = new GameInfoCell();
            cell.getStyleClass().add("gameTile");
            cell.setOnMouseClicked(event -> {
                final Game game = cell.getItem();
                selectedGameProperty.set(game);
                event.consume();
            });

            final ContextMenu contextMenu = createContextMenu(cell);
            cell.setContextMenu(contextMenu);
            return cell;
        });
    }

    private ContextMenu createContextMenu(GameInfoCell cell) {
        final ContextMenu contextMenu = new ContextMenu();

        final MenuItem deleteItem = new MenuItem("Delete");
        deleteItem.setOnAction(e -> {
            final Game game = cell.getItem();
            // TODO: Confirmation menu. And go through flowManager.
            deletedGameProperty.set(game);
        });

        contextMenu.getItems().addAll(deleteItem);
        return contextMenu;
    }
}

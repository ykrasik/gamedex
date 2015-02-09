package com.github.ykrasik.indexter.games.ui;

import com.github.ykrasik.indexter.games.datamodel.info.SearchResult;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;
import org.controlsfx.control.ButtonBar.ButtonType;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.DialogAction;

import java.util.Optional;

import static com.github.ykrasik.indexter.optional.Optionals.toStringOrUnavailable;

/**
 * @author Yevgeny Krasik
 */
public class SearchResultDialog<T extends SearchResult> {
    private final TableView<T> tableView = new TableView<>();

    private Stage stage;
    private String title = "Search Results:";

    // TODO: Should display path as well.
    public SearchResultDialog() {
        final TableColumn<T, String> nameColumn = new TableColumn<>("Name");
        final TableColumn<T, String> releaseDateColumn = new TableColumn<>("Release Date");
        final TableColumn<T, String> scoreColumn = new TableColumn<>("Score");
        tableView.getColumns().addAll(nameColumn, releaseDateColumn, scoreColumn);

        nameColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getName()));
        releaseDateColumn.setCellValueFactory(e -> new SimpleStringProperty(toStringOrUnavailable(e.getValue().getReleaseDate())));
        scoreColumn.setCellValueFactory(e -> new SimpleStringProperty(toStringOrUnavailable(e.getValue().getScore())));
    }

    public SearchResultDialog<T> owner(Stage stage) {
        this.stage = stage;
        return this;
    }

    public SearchResultDialog<T> title(String title) {
        this.title = title;
        return this;
    }

    public Optional<T> show(ObservableList<T> items) {
        final Dialog dialog = new Dialog(stage, title);
        final DialogAction actionOk = new DialogAction("OK", ButtonType.OK_DONE);

        tableView.setItems(items);

        tableView.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.isPrimaryButtonDown() && event.getClickCount() == 2) {
                    // Translate double-click to 'OK' button press.
                    actionOk.handle(new ActionEvent(dialog, event.getTarget()));
                }
            }
        });

        dialog.setContent(tableView);
        dialog.setIconifiable(false);
        dialog.getActions().addAll(actionOk, Dialog.ACTION_CANCEL);

        final Action response = dialog.show();
        if (response == actionOk) {
            return Optional.of(getSelectedItem());
        } else {
            return Optional.empty();
        }
    }

    private T getSelectedItem() {
        return tableView.selectionModelProperty().get().getSelectedItem();
    }
}

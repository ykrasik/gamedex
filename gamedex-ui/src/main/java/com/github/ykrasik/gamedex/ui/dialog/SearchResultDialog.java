package com.github.ykrasik.gamedex.ui.dialog;

import com.github.ykrasik.gamedex.datamodel.info.SearchResult;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import org.controlsfx.control.ButtonBar.ButtonType;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.DialogAction;

import java.nio.file.Path;
import java.util.Optional;

import static com.github.ykrasik.gamedex.common.optional.Optionals.toStringOrUnavailable;

/**
 * @author Yevgeny Krasik
 */
public class SearchResultDialog {
    private final BorderPane content = new BorderPane();
    private final Label pathLabel = new Label();
    private final TableView<SearchResult> tableView = new TableView<>();

    private Stage stage;
    private String title = "Search Results:";

    public SearchResultDialog() {
        final TableColumn<SearchResult, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getName()));

        final TableColumn<SearchResult, String> releaseDateColumn = new TableColumn<>("Release Date");
        releaseDateColumn.setCellValueFactory(e -> new SimpleStringProperty(toStringOrUnavailable(e.getValue().getReleaseDate())));

        final TableColumn<SearchResult, String> scoreColumn = new TableColumn<>("Score");
        scoreColumn.setCellValueFactory(e -> new SimpleStringProperty(toStringOrUnavailable(e.getValue().getScore())));

        tableView.getColumns().addAll(nameColumn, releaseDateColumn, scoreColumn);

        pathLabel.setWrapText(true);
        pathLabel.setAlignment(Pos.CENTER);
        BorderPane.setAlignment(pathLabel, Pos.CENTER);

        content.getStyleClass().add("masthead-panel"); //$NON-NLS-1$
        content.setTop(pathLabel);
        content.setCenter(tableView);
    }

    public SearchResultDialog owner(Stage stage) {
        this.stage = stage;
        return this;
    }

    public SearchResultDialog title(String title) {
        this.title = title;
        return this;
    }

    public Optional<SearchResult> show(Path path, ObservableList<SearchResult> items) {
        final Dialog dialog = new Dialog(stage, title);
        final DialogAction actionOk = new DialogAction("OK", ButtonType.OK_DONE);

        pathLabel.setText(path.toString());
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

        dialog.setMasthead(content);
        dialog.setIconifiable(false);
        dialog.getActions().addAll(actionOk, Dialog.ACTION_CANCEL);

        final Action response = dialog.show();
        if (response == actionOk) {
            return Optional.of(getSelectedItem());
        } else {
            return Optional.empty();
        }
    }

    private SearchResult getSelectedItem() {
        return tableView.selectionModelProperty().get().getSelectedItem();
    }
}

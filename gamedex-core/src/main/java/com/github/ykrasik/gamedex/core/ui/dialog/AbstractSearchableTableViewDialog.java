package com.github.ykrasik.gamedex.core.ui.dialog;

import com.github.ykrasik.opt.Opt;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.controlsfx.control.ButtonBar.ButtonType;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;
import org.controlsfx.dialog.DialogAction;

import java.util.Collection;

/**
 * @author Yevgeny Krasik
 */
@Accessors(fluent = true)
public abstract class AbstractSearchableTableViewDialog<T> {
    private final TextField searchTextField = new TextField();
    private final GridPane content = new GridPane();
    private final TableView<T> tableView = new TableView<>();
    private final DialogAction actionOk = new DialogAction("OK", ButtonType.OK_DONE);

    @Setter private Stage owner;
    @Setter @NonNull private String title = "";
    @Setter private T previouslySelectedItem;

    protected AbstractSearchableTableViewDialog() {
        final Collection<? extends TableColumn<T, ?>> columns = getColumns();
        tableView.getColumns().addAll(columns);

        content.setHgap(10);
        content.setVgap(10);
        content.add(new Label("Search:"), 0, 0);
        content.add(searchTextField, 1, 0);
        GridPane.setHgrow(searchTextField, Priority.ALWAYS);
        content.add(tableView, 0, 1, 2, 1);
        GridPane.setVgrow(tableView, Priority.ALWAYS);
    }

    protected abstract Collection<? extends TableColumn<T, ?>> getColumns();

    public Opt<T> show(ObservableList<T> items) {
        final Dialog dialog = createDialog(items);

        Platform.runLater(searchTextField::requestFocus);

        final Action response = dialog.show();
        if (response == actionOk) {
            // It is possible to press "OK" without selecting anything.
            return Opt.ofNullable(tableView.selectionModelProperty().get().getSelectedItem());
        } else {
            return Opt.absent();
        }
    }

    protected Dialog createDialog(ObservableList<T> items) {
        final Dialog dialog = new Dialog(owner, title);

        tableView.setItems(items);
        if (previouslySelectedItem != null) {
            tableView.selectionModelProperty().get().select(previouslySelectedItem);
        }

        tableView.setOnMousePressed(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                if (event.isPrimaryButtonDown() && event.getClickCount() == 2) {
                    // Translate double-click to 'OK' button press.
                    actionOk.handle(new ActionEvent(dialog, event.getTarget()));
                }
            }
        });

        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            final ObservableList<T> filtered;
            if (newValue.isEmpty()) {
                filtered = items;
            } else {
                filtered = items.filtered(item -> doesItemMatchSearch(item, newValue));
            }
            tableView.setItems(filtered);
        });

        dialog.setContent(content);
        dialog.setResizable(true);
        dialog.getActions().addAll(actionOk, Dialog.ACTION_CANCEL);
        return dialog;
    }

    protected abstract boolean doesItemMatchSearch(T item, String search);
}

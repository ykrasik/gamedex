package com.github.ykrasik.gamedex.core.ui.dialog;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;

import java.util.List;
import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class SearchableCheckListViewDialog<T> {
    private final TextField searchTextField = new TextField();
    private final CheckListView<T> checkListView = new CheckListView<>();
    private final GridPane content = new GridPane();

    private Stage stage;
    private String title = "Search Results:";

    public SearchableCheckListViewDialog() {
        content.setHgap(10);
        content.setVgap(10);
        content.add(new Label("Search:"), 0, 0);
        content.add(searchTextField, 1, 0);
        GridPane.setHgrow(searchTextField, Priority.ALWAYS);
        content.add(checkListView, 0, 1, 2, 1);
        GridPane.setVgrow(checkListView, Priority.ALWAYS);
    }

    public SearchableCheckListViewDialog<T> owner(Stage stage) {
        this.stage = stage;
        return this;
    }

    public SearchableCheckListViewDialog<T> title(String title) {
        this.title = title;
        return this;
    }

    public Optional<List<T>> show(ObservableList<T> items) {
        final Dialog dialog = new Dialog(stage, title);

        checkListView.setItems(items);

        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            final ObservableList<T> checkedItems = checkListView.checkModelProperty().get().getCheckedItems();
            final ObservableList<T> mergedItems;
            if (newValue.isEmpty()) {
                mergedItems = items;
            } else {
                mergedItems = FXCollections.observableArrayList(checkedItems);
                mergedItems.addAll(items.filtered(item -> StringUtils.containsIgnoreCase(item.toString(), newValue)));
            }
            checkListView.setItems(mergedItems);
            for (T checkedItem : checkedItems) {
                checkListView.checkModelProperty().get().check(checkedItem);
            }
        });

        dialog.setContent(content);
        dialog.setResizable(false);
        dialog.getActions().addAll(Dialog.ACTION_OK, Dialog.ACTION_CANCEL);

        Platform.runLater(searchTextField::requestFocus);

        final Action response = dialog.show();
        if (response == Dialog.ACTION_OK) {
            return Optional.of(checkListView.checkModelProperty().get().getCheckedItems());
        } else {
            return Optional.empty();
        }
    }
}

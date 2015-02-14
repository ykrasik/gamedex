package com.github.ykrasik.gamedex.core.ui.dialog;

import com.github.ykrasik.opt.Opt;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.controlsfx.control.CheckListView;
import org.controlsfx.control.IndexedCheckModel;
import org.controlsfx.control.action.Action;
import org.controlsfx.dialog.Dialog;

import java.util.Collections;
import java.util.List;

/**
 * @author Yevgeny Krasik
 */
@Accessors(fluent = true)
public abstract class AbstractSearchableCheckListViewDialog<T> {
    private final TextField searchTextField = new TextField();
    private final CheckListView<T> checkListView = new CheckListView<>();
    private final GridPane content = new GridPane();

    @Setter private Stage owner;
    @Setter @NonNull private String title = "";
    @Setter @NonNull private List<T> previouslyCheckedItems = Collections.emptyList();

    protected AbstractSearchableCheckListViewDialog() {
        content.setHgap(10);
        content.setVgap(10);
        content.add(new Label("Search:"), 0, 0);
        content.add(searchTextField, 1, 0);
        GridPane.setHgrow(searchTextField, Priority.ALWAYS);
        content.add(checkListView, 0, 1, 2, 1);
        GridPane.setVgrow(checkListView, Priority.ALWAYS);
    }

    public Opt<List<T>> show(ObservableList<T> items) {
        final Dialog dialog = new Dialog(owner, title);

        checkListView.setItems(items);
        final IndexedCheckModel<T> checkModel = checkListView.checkModelProperty().get();
        previouslyCheckedItems.forEach(checkModel::check);

        searchTextField.textProperty().addListener((observable, oldValue, newValue) -> {
            final ObservableList<T> checkedItems = checkModel.getCheckedItems();
            final ObservableList<T> mergedItems;
            if (newValue.isEmpty()) {
                mergedItems = items;
            } else {
                mergedItems = FXCollections.observableArrayList(checkedItems);
                mergedItems.addAll(items.filtered(item -> doesMatchSearch(item, newValue)));
            }
            checkListView.setItems(mergedItems);
            checkedItems.forEach(checkModel::check);
        });

        dialog.setContent(content);
        dialog.setResizable(false);
        dialog.getActions().addAll(Dialog.ACTION_OK, Dialog.ACTION_CANCEL);

        Platform.runLater(searchTextField::requestFocus);

        final Action response = dialog.show();
        if (response == Dialog.ACTION_OK) {
            final ObservableList<T> checkedItems = checkListView.checkModelProperty().get().getCheckedItems();
            if (checkedItems.isEmpty()) {
                return Opt.absent();
            } else {
                return Opt.of(checkedItems);
            }
        } else {
            return Opt.absent();
        }
    }

    protected abstract boolean doesMatchSearch(T item, String search);
}

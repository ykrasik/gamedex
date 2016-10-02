package com.github.ykrasik.gamedex.core.ui.dialog;

import com.gitlab.ykrasik.gamedex.provider.SearchResult;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.controlsfx.dialog.Dialog;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;

import static com.github.ykrasik.gamedex.common.util.StringUtils.toStringOrUnavailable;

/**
 * @author Yevgeny Krasik
 */
public class SearchResultsDialog extends AbstractSearchableTableViewDialog<SearchResult> {
    private final Path path;

    public SearchResultsDialog(@NonNull String providerName,
                               @NonNull String name,
                               @NonNull Path path) {
        this.path = path;
        title(String.format("%s search results for: '%s'", providerName, name));
    }

    @Override
    protected Collection<? extends TableColumn<SearchResult, ?>> getColumns() {
        final TableColumn<SearchResult, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getName()));

        final TableColumn<SearchResult, String> releaseDateColumn = new TableColumn<>("Release Date");
        releaseDateColumn.setCellValueFactory(e -> new SimpleStringProperty(toStringOrUnavailable(e.getValue().getReleaseDate())));

        final TableColumn<SearchResult, String> scoreColumn = new TableColumn<>("Score");
        scoreColumn.setCellValueFactory(e -> new SimpleStringProperty(toStringOrUnavailable(e.getValue().getScore())));

        return Arrays.asList(nameColumn, releaseDateColumn, scoreColumn);
    }

    @Override
    protected boolean doesItemMatchSearch(SearchResult item, String search) {
        return StringUtils.containsIgnoreCase(item.getName(), search);
    }

    @Override
    protected Dialog createDialog(ObservableList<SearchResult> items) {
        final Label pathLabel = new Label();
        pathLabel.setText(path.toString());

        final Dialog dialog = super.createDialog(items);
        dialog.setMasthead(pathLabel);
        return dialog;
    }
}

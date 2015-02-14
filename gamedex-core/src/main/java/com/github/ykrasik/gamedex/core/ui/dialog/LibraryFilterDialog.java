package com.github.ykrasik.gamedex.core.ui.dialog;

import com.github.ykrasik.gamedex.datamodel.persistence.Library;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.control.TableColumn;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.Collection;

/**
 * @author Yevgeny Krasik
 */
public class LibraryFilterDialog extends AbstractSearchableTableViewDialog<Library> {
    public LibraryFilterDialog() {
        title("Select Library");
    }

    @Override
    protected Collection<? extends TableColumn<Library, ?>> getColumns() {
        final TableColumn<Library, String> nameColumn = new TableColumn<>("Name");
        nameColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getName()));

        final TableColumn<Library, String> pathColumn = new TableColumn<>("Path");
        pathColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPath().toString()));

        final TableColumn<Library, String> platformColumn = new TableColumn<>("Platform");
        platformColumn.setCellValueFactory(e -> new SimpleStringProperty(e.getValue().getPlatform().getKey()));

        return Arrays.asList(nameColumn, pathColumn, platformColumn);
    }

    @Override
    protected boolean doesMatchSearch(Library item, String search) {
        return StringUtils.containsIgnoreCase(item.getName(), search);
    }
}

package com.github.ykrasik.gamedex.core.ui.dialog;

import com.github.ykrasik.gamedex.datamodel.persistence.Genre;
import org.apache.commons.lang3.StringUtils;

/**
 * @author Yevgeny Krasik
 */
public class GenreFilterDialog extends AbstractSearchableCheckListViewDialog<Genre> {
    public GenreFilterDialog() {
        title("Select Genres");
    }

    @Override
    protected boolean doesItemMatchSearch(Genre item, String search) {
        return StringUtils.containsIgnoreCase(item.getName(), search);
    }
}

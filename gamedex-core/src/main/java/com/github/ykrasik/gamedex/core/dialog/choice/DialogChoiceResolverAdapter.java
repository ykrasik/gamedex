package com.github.ykrasik.gamedex.core.dialog.choice;

import com.github.ykrasik.gamedex.core.flow.ExcludeException;
import com.github.ykrasik.gamedex.core.flow.SkipException;
import com.github.ykrasik.gamedex.datamodel.info.GameInfo;
import com.github.ykrasik.gamedex.datamodel.info.SearchResult;

import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class DialogChoiceResolverAdapter implements DialogChoiceResolver {
    @Override
    public Optional<GameInfo> skip() {
        throw new SkipException();
    }

    @Override
    public Optional<GameInfo> exclude() {
        throw new ExcludeException();
    }

    @Override
    public Optional<GameInfo> proceedAnyway() {
        return Optional.empty();
    }

    @Override
    public Optional<GameInfo> newName(String newName) throws Exception {
        throw unsupported("Retry with new name");
    }

    @Override
    public Optional<GameInfo> choose(SearchResult chosenSearchResult) throws Exception {
        throw unsupported("Choose from search results");
    }

    private RuntimeException unsupported(String choice) {
        throw new UnsupportedOperationException(String.format("'%s' is not a valid choice!", choice));
    }
}

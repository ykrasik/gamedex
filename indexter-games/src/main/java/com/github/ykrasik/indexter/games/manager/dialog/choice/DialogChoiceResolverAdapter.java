package com.github.ykrasik.indexter.games.manager.dialog.choice;

import com.github.ykrasik.indexter.games.datamodel.info.GameInfo;
import com.github.ykrasik.indexter.games.datamodel.info.SearchResult;
import com.github.ykrasik.indexter.games.manager.flow.ExcludeException;
import com.github.ykrasik.indexter.games.manager.flow.SkipException;

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

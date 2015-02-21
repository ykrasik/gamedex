package com.github.ykrasik.gamedex.core.service.dialog.choice;

import com.github.ykrasik.gamedex.core.service.action.ExcludeException;
import com.github.ykrasik.gamedex.core.service.action.SkipException;
import com.github.ykrasik.gamedex.datamodel.provider.GameInfo;
import com.github.ykrasik.gamedex.datamodel.provider.SearchResult;
import com.github.ykrasik.opt.Opt;

/**
 * @author Yevgeny Krasik
 */
public class DefaultDialogChoiceResolver implements DialogChoiceResolver {
    @Override
    public Opt<GameInfo> skip() {
        throw new SkipException();
    }

    @Override
    public Opt<GameInfo> exclude() {
        throw new ExcludeException();
    }

    @Override
    public Opt<GameInfo> proceedAnyway() {
        return Opt.absent();
    }

    @Override
    public Opt<GameInfo> newName(String newName) throws Exception {
        throw unsupported("Retry with new name");
    }

    @Override
    public Opt<GameInfo> choose(SearchResult chosenSearchResult) throws Exception {
        throw unsupported("Choose from search results");
    }

    private RuntimeException unsupported(String choice) {
        throw new UnsupportedOperationException(String.format("'%s' is not a valid choice!", choice));
    }
}

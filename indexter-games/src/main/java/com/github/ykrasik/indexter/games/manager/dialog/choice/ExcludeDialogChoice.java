package com.github.ykrasik.indexter.games.manager.dialog.choice;

import com.github.ykrasik.indexter.games.datamodel.info.GameInfo;

import java.util.Optional;

/**
 * @author Yevgeny Krasik
 */
public class ExcludeDialogChoice implements DialogChoice {
    private static final ExcludeDialogChoice INSTANCE = new ExcludeDialogChoice();

    public static ExcludeDialogChoice instance() {
        return INSTANCE;
    }

    private ExcludeDialogChoice() { }

    @Override
    public Optional<GameInfo> resolve(DialogChoiceResolver resolver) {
        return resolver.exclude();
    }
}

package com.github.ykrasik.gamedex.core.dialog.choice;

import com.github.ykrasik.gamedex.datamodel.info.GameInfo;

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
